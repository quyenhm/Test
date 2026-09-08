/* groovylint-disable LineLength */

void sendEmail(Map data = [:]) {
    boolean showTests = data.get('showTests', true)

    if (data.email) {
        boolean hasTestCounts = data.totalTests != null

        // ── Status detection — three states now: pass / unstable / fail.
        //    Checked primarily against the human-readable `result` text (most
        //    robust across callers), falling back to the `color` hex/keyword. ──
        String resultLower = (data.result ?: '').toString().toLowerCase()
        String colorLower  = (data.color ?: '').toString().toLowerCase()
        boolean isFail = resultLower.contains('fail') ||
            colorLower in ['#e8563f', 'red', 'danger', 'error', 'failure']
        boolean isUnstable = !isFail && (
            resultLower.contains('unstable') ||
            colorLower in ['#f5ba45', 'amber', 'yellow', 'warning', 'unstable']
        )
        String statusType = isFail ? 'fail' : (isUnstable ? 'unstable' : 'pass')

        // ── Palette — flattened from TestReport.template.html's dark ("--*") tokens.
        //    Email clients don't support CSS custom properties or oklch(), so every
        //    value below is the closest hex equivalent, pre-mixed against the dark
        //    background where the original used an alpha overlay. ──
        String bg           = '#0a0a0a'
        String surface      = '#111111'
        String surface2     = '#161616'
        String border       = '#262626'
        String borderStrong = '#333333'
        String text         = '#ededed'
        String textMuted    = '#a3a3a3'
        String textDim      = '#737373'
        String accent       = '#818cf8'   // indigo — brand mark / links
        String pass         = '#4ade80'
        String passSoft     = '#122317'
        String passBorder   = '#1f3d2a'
        String fail         = '#fb7185'
        String failSoft     = '#2a1418'
        String failBorder   = '#3d1f26'
        String skip         = '#fbbf24'   // shared with "unstable" — same warning family as the Skipped stat
        String skipSoft     = '#2a2410'
        String skipBorder   = '#3d3417'

        // ── Status icon — Fluent-style rounded-stroke glyphs (matches Segoe Fluent
        //    Icons' geometric construction: 2.5px stroke, round caps/joins) instead
        //    of a plain Unicode character.
        //    Inline SVG isn't supported by classic Windows Outlook desktop (it uses
        //    Word's rendering engine), so we hide the SVG from mso and show a plain
        //    Unicode fallback there instead — the standard "hide-from-Outlook" /
        //    "Outlook-only" conditional-comment pair. ──
        String fluentCheckmarkSvg = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M4.5 12.5l5 5L19.5 7" stroke="#0a0a0a" stroke-width="2.75" stroke-linecap="round" stroke-linejoin="round"/></svg>'
        String fluentDismissSvg   = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M6 6l12 12M18 6L6 18" stroke="#0a0a0a" stroke-width="2.75" stroke-linecap="round" stroke-linejoin="round"/></svg>'
        String fluentWarningSvg   = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M12 7v6.5" stroke="#0a0a0a" stroke-width="2.75" stroke-linecap="round"/><circle cx="12" cy="17" r="1.4" fill="#0a0a0a"/></svg>'

        Map statusStyles = [
            pass:     [color: pass, soft: passSoft, border: passBorder, icon: fluentCheckmarkSvg, mso: '&#10003;', label: 'All Passing'],
            unstable: [color: skip, soft: skipSoft, border: skipBorder, icon: fluentWarningSvg,   mso: '!',        label: 'Unstable'],
            fail:     [color: fail, soft: failSoft, border: failBorder, icon: fluentDismissSvg,   mso: '&#10007;', label: 'Build Failed'],
        ]
        Map st = statusStyles[statusType]
        String statusColor  = st.color
        String statusSoft   = st.soft
        String statusBorder = st.border
        String statusLabel  = data.result ?: st.label
        String statusIcon   = "<!--[if mso]>${st.mso}<![endif]--><!--[if !mso]><!-->${st.icon}<!--<![endif]-->"

        String monoStack = "'JetBrains Mono', 'Courier New', ui-monospace, monospace"
        String sansStack = "'Inter', 'Segoe UI', Tahoma, Arial, sans-serif"

        // ── Pass-rate bar + legend (stands in for the SVG donut — table/div based,
        //    so it survives clients that strip inline SVG). ──
        String donutBlock = ''
        if (hasTestCounts && showTests) {
            int totalN = (data.totalTests as Integer) ?: 0
            int passedN = (data.passedTests as Integer) ?: 0
            int failedN = (data.failedTests as Integer) ?: 0
            int skippedN = (data.skippedTests as Integer) ?: 0
            int passRate = totalN > 0 ? Math.round((passedN / (double) totalN) * 100) as int : 0
            String rateColor = passRate == 100 ? pass : (passRate >= 90 ? skip : fail)

            int wPass = totalN > 0 ? Math.round((passedN / (double) totalN) * 100) as int : 0
            int wFail = totalN > 0 ? Math.round((failedN / (double) totalN) * 100) as int : 0
            int wSkip = Math.max(0, 100 - wPass - wFail)

            donutBlock = """
            <tr>
              <td class="email-pad" style="padding:20px 24px 0 24px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:${surface2};border:1px solid ${border};border-radius:12px;">
                  <tr>
                    <td style="padding:18px 20px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                          <td valign="bottom">
                            <div style="font-family:${monoStack};font-size:30px;font-weight:700;color:${rateColor};line-height:1;">${passRate}<span style="font-size:16px;color:${textDim};">%</span></div>
                            <div style="font-size:10px;font-weight:700;letter-spacing:.10em;text-transform:uppercase;color:${textDim};margin-top:4px;">Pass Rate</div>
                          </td>
                        </tr>
                      </table>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin-top:14px;">
                        <tr>
                          <td style="height:10px;line-height:10px;font-size:0;border-radius:4px;overflow:hidden;">${wPass > 0 ? "<span style=\"display:inline-block;height:10px;width:${wPass}%;background:${pass};\">&nbsp;</span>" : ''}${wFail > 0 ? "<span style=\"display:inline-block;height:10px;width:${wFail}%;background:${fail};\">&nbsp;</span>" : ''}${wSkip > 0 ? "<span style=\"display:inline-block;height:10px;width:${wSkip}%;background:${skip};\">&nbsp;</span>" : ''}${(wPass + wFail + wSkip) == 0 ? "<span style=\"display:inline-block;height:10px;width:100%;background:${border};\">&nbsp;</span>" : ''}</td>
                        </tr>
                      </table>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin-top:14px;">
                        <tr>
                          <td width="33%" style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${pass};margin-right:6px;"></span>Passed
                            <span style="font-family:${monoStack};color:${text};font-weight:700;float:right;">${passedN}</span>
                          </td>
                        </tr>
                        <tr><td style="height:6px;font-size:0;">&nbsp;</td></tr>
                        <tr>
                          <td style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${fail};margin-right:6px;"></span>Failed
                            <span style="font-family:${monoStack};color:${text};font-weight:700;float:right;">${failedN}</span>
                          </td>
                        </tr>
                        <tr><td style="height:6px;font-size:0;">&nbsp;</td></tr>
                        <tr>
                          <td style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${skip};margin-right:6px;"></span>Skipped
                            <span style="font-family:${monoStack};color:${text};font-weight:700;float:right;">${skippedN}</span>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            """.stripIndent()
        }

        // ── Stat cards (Total / Passed / Failed / Skipped) — mirrors the dashboard's
        //    .stat cards: dark surface, colored top rule, mono number, dim label. ──
        String testSummaryBlock = ''
        if (hasTestCounts && showTests) {
            List<Map> stats = [
                [label: 'Total',   value: data.totalTests,   color: textDim, bgc: surface2, bord: border],
                [label: 'Passed',  value: data.passedTests,  color: pass,    bgc: passSoft,  bord: passBorder],
                [label: 'Failed',  value: data.failedTests,  color: fail,    bgc: failSoft,  bord: failBorder],
                [label: 'Skipped', value: data.skippedTests, color: skip,    bgc: skipSoft,  bord: skipBorder],
            ]
            String cells = stats.collect { s -> """
              <td class="stat-cell" width="25%" style="padding:4px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:${s.bgc};border:1px solid ${s.bord};border-top:2px solid ${s.color};border-radius:8px;">
                  <tr><td align="center" style="padding:14px 6px;">
                    <div style="font-family:${monoStack};font-size:21px;font-weight:700;color:${s.color};line-height:1.1;">${s.value}</div>
                    <div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:${textDim};margin-top:5px;">${s.label}</div>
                  </td></tr>
                </table>
              </td>
            """ }.join('')
            testSummaryBlock = """
            <tr>
              <td class="email-pad" style="padding:20px 24px 0 24px;">
                <p style="margin:0 0 10px 0;font-size:11px;font-weight:700;letter-spacing:.10em;text-transform:uppercase;color:${textDim};">Test Summary</p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>${cells}</tr>
                </table>
              </td>
            </tr>
            """.stripIndent()
        }

        // ── Meta strip: Started / Duration / Job / Branch / Build — mono values,
        //    uppercase dim labels, matches .meta-strip in the run view.
        //    Job prefers the explicit `data.jobName` (callers now pass this
        //    directly); falls back to parsing env.JOB_NAME for older callers. ──
        String jobDisplay = data.jobName ?: {
            try { return env.JOB_NAME.tokenize('/').dropRight(1).last() }
            catch (ignored) { return env.JOB_NAME ?: '—' }
        }()
        List<List<String>> metaRows = [
            ['Started', "${data.startTime}"],
            ['Duration', "${currentBuild.durationString}"],
            ['Job', "${jobDisplay}"],
            ['Branch', "${env.BRANCH_NAME}"],
            ['Build', "#${env.BUILD_NUMBER}"],
        ]
        String metaCells = metaRows.collect { pair -> """
          <td class="meta-cell" width="50%" valign="top" style="padding:10px 12px;background:${surface2};border:1px solid ${border};">
            <div style="font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:${textDim};">${pair[0]}</div>
            <div style="font-family:${monoStack};font-size:12px;color:${text};margin-top:4px;word-break:break-word;">${pair[1]}</div>
          </td>
        """ }.join('')

        // ── Quick links — solid accent button for the primary link, outlined
        //    "pill" style (matching .pill / .sort-btn) for the rest. ──
        List<Map> links = [
            [label: 'Build Information', url: env.BUILD_URL, primary: true],
            [label: 'Pipeline Overview', url: "${env.BUILD_URL}pipeline-overview", primary: false],
            [label: 'Console Output', url: "${env.BUILD_URL}console", primary: false],
        ]
        if (showTests) {
            links << [label: 'Test Results', url: "${env.BUILD_URL}testReport", primary: false]
        }
        String linksBlock = links.collect { link ->
            String bgc = link.primary ? accent : surface2
            String fg = link.primary ? '#0a0a0a' : text
            String bd = link.primary ? accent : borderStrong
            """
            <tr>
              <td style="padding:0 0 8px 0;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td align="center" bgcolor="${bgc}" style="border-radius:7px;border:1px solid ${bd};">
                      <a href="${link.url}" target="_blank" style="display:block;padding:11px 16px;font-size:13px;font-weight:600;color:${fg};text-decoration:none;font-family:${sansStack};">${link.label}</a>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            """.stripIndent()
        }.join('')

        String body = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="color-scheme" content="dark">
        <meta name="supported-color-schemes" content="dark">
        <title>${data.title}</title>
        <!--[if mso]>
        <style type="text/css">
          table, td { font-family: Arial, sans-serif !important; }
        </style>
        <![endif]-->
        <style>
          body, table, td, a { -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%; }
          table, td { mso-table-lspace:0pt; mso-table-rspace:0pt; }
          body { margin:0; padding:0; width:100% !important; height:100% !important; }

          @media screen and (max-width: 600px) {
            .email-wrap { width:100% !important; }
            .email-pad { padding-left:16px !important; padding-right:16px !important; }
            .stat-cell { display:inline-block !important; width:48% !important; }
            .meta-cell { display:block !important; width:100% !important; }
            .header-title { font-size:18px !important; }
          }
        </style>
        </head>
        <body style="margin:0;padding:0;background-color:${bg};font-family:${sansStack};">
          <div style="display:none;max-height:0;overflow:hidden;opacity:0;">${statusLabel} · ${currentBuild.fullDisplayName} · ${currentBuild.durationString}</div>
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:${bg};">
            <tr>
              <td align="center" style="padding:28px 12px;">
                <table role="presentation" class="email-wrap" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;background-color:${surface};border-radius:14px;overflow:hidden;border:1px solid ${border};">

                  <!-- Brand row -->
                  <tr>
                    <td class="email-pad" style="padding:22px 24px 16px 24px;border-bottom:1px solid ${border};">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                          <td width="38" valign="middle">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td width="34" height="34" align="center" valign="middle" bgcolor="${accent}" style="border-radius:8px;font-family:${sansStack};font-size:15px;font-weight:700;color:#0a0a0a;">CI</td>
                              </tr>
                            </table>
                          </td>
                          <td valign="middle" style="padding-left:12px;">
                            <div style="font-size:14px;font-weight:700;color:${text};letter-spacing:-.01em;">Jenkins CI</div>
                            <div style="font-size:10px;font-weight:600;letter-spacing:.08em;text-transform:uppercase;color:${textDim};margin-top:1px;">Test Report</div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Status pill -->
                  <tr>
                    <td class="email-pad" style="padding:20px 24px 0 24px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="background:${statusSoft};border:1px solid ${statusBorder};border-radius:999px;">
                        <tr>
                          <td style="padding:8px 16px 8px 10px;">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td width="22" height="22" align="center" valign="middle" bgcolor="${statusColor}" style="border-radius:50%;font-size:11px;color:#0a0a0a;font-weight:700;">${statusIcon}</td>
                                <td style="padding-left:9px;font-size:13px;font-weight:700;color:${statusColor};white-space:nowrap;">${statusLabel}</td>
                                ${(hasTestCounts && showTests) ? """
                                <td style="padding-left:10px;border-left:1px solid ${statusBorder};">
                                  <span style="display:inline-block;padding-left:10px;font-family:${monoStack};font-size:11px;color:${textMuted};white-space:nowrap;">${data.passedTests}/${data.totalTests} passed &nbsp;·&nbsp; ${currentBuild.durationString}</span>
                                </td>
                                """ : ''}
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                      <div style="font-size:12px;color:${textDim};margin-top:8px;">${currentBuild.fullDisplayName}</div>
                    </td>
                  </tr>

                  <!-- Meta strip -->
                  <tr>
                    <td class="email-pad" style="padding:20px 24px 0 24px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border-collapse:separate;border-spacing:1px;">
                        <tr>${metaCells}</tr>
                      </table>
                    </td>
                  </tr>

                  ${donutBlock}

                  ${testSummaryBlock}

                  <!-- Quick links -->
                  <tr>
                    <td class="email-pad" style="padding:24px 24px 4px 24px;">
                      <p style="margin:0 0 10px 0;font-size:11px;font-weight:700;letter-spacing:.10em;text-transform:uppercase;color:${textDim};">Quick Links</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                        ${linksBlock}
                      </table>
                    </td>
                  </tr>

                  <!-- Message -->
                  ${data.message ? """
                  <tr>
                    <td class="email-pad" style="padding:8px 24px 0 24px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:${surface2};border:1px solid ${border};border-radius:8px;">
                        <tr><td style="padding:12px 14px;font-size:12px;line-height:1.6;color:${textMuted};">${data.message}</td></tr>
                      </table>
                    </td>
                  </tr>
                  """ : ''}

                  <!-- Footer -->
                  <tr>
                    <td class="email-pad" style="padding:22px 24px 24px 24px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr><td style="border-top:1px solid ${border};padding-top:16px;">
                          <p style="margin:0;font-size:11px;color:${textDim};line-height:1.6;">
                            Regards,<br>
                            <span style="color:${textMuted};font-weight:600;">Jenkins CI</span>
                          </p>
                        </td></tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.stripIndent().trim()

        try {
            mail(
                to: data.email,
                subject: "${data.title}: ${currentBuild.fullDisplayName}",
                mimeType: 'text/html',
                body: body
            )
        } catch (err) {
            echo "Failed to send email to '${data.email}'. Error: ${err}"
        }
    }
    else {
        echo 'No notification email configured ($ctx.email is missing or empty).'
    }
}

return this

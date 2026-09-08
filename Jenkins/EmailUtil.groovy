/* groovylint-disable LineLength */

void sendEmail(Map data = [:]) {
    boolean showTests = data.get('showTests', true)

    if (data.email) {
        boolean hasTestCounts = data.totalTests != null

        // ── Status classification — pass / unstable / fail, derived from the
        //    hex/keyword passed in `data.color`. Falls back to 'fail' (most
        //    urgent styling) for anything unrecognised. ──
        String colorIn = (data.color ?: '').toString().toLowerCase()
        boolean isPass = colorIn in ['#4caf50', 'green', 'success']
        boolean isWarn = colorIn in ['#f5ba45', '#ffc107', 'amber', 'yellow', 'warning', 'unstable']
        String statusType = isPass ? 'pass' : (isWarn ? 'warn' : 'fail')

        // ── Palette — no page-level theme. Only small, meaningful indicator
        //    elements (status pill, stat cards) carry a soft tint; everything
        //    else (page, container, meta cells, message box) is left with NO
        //    background color at all, so the email client's own light/dark
        //    mode handles inversion instead of us fighting it with a forced
        //    theme. ──
        String border       = '#e5e7eb'
        String borderStrong = '#d1d5db'
        String textDim      = '#9ca3af'
        String textMuted    = '#6b7280'
        String iconInk      = '#111827'   // dark stroke for icons on light badges
        String accent       = '#6366f1'   // indigo — brand mark / primary CTA

        String pass       = '#16a34a'
        String passSoft   = '#f0fdf4'
        String passBorder = '#bbf7d0'
        String fail        = '#dc2626'
        String failSoft    = '#fef2f2'
        String failBorder  = '#fecaca'
        String warn        = '#d97706'
        String warnSoft     = '#fffbeb'
        String warnBorder   = '#fde68a'
        String skip        = '#d97706'   // shares the amber family with "warn"

        Map statusStyle = [
            pass: [color: pass, soft: passSoft, border: passBorder],
            warn: [color: warn, soft: warnSoft, border: warnBorder],
            fail: [color: fail, soft: failSoft, border: failBorder],
        ][statusType]
        String statusColor  = statusStyle.color
        String statusSoft   = statusStyle.soft
        String statusBorder = statusStyle.border
        String statusLabel  = data.result ?: (isPass ? 'All Passing' : (isWarn ? 'Unstable' : 'Build Failed'))

        // ── Status icon — Fluent-style rounded-stroke glyph (2.5px stroke, round
        //    caps/joins, matching Segoe Fluent Icons' construction). Classic
        //    Windows Outlook desktop (Word rendering engine) can't render inline
        //    SVG, so it gets a plain-text fallback via MSO conditional comments;
        //    every other client (Apple Mail, Gmail, new Outlook, mobile apps)
        //    gets the real vector icon. ──
        String fluentCheckmarkSvg = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M4.5 12.5l5 5L19.5 7" stroke="' + iconInk + '" stroke-width="2.75" stroke-linecap="round" stroke-linejoin="round"/></svg>'
        String fluentDismissSvg   = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M6 6l12 12M18 6L6 18" stroke="' + iconInk + '" stroke-width="2.75" stroke-linecap="round" stroke-linejoin="round"/></svg>'
        String fluentWarningSvg   = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block;"><path d="M12 7.5v6" stroke="' + iconInk + '" stroke-width="2.75" stroke-linecap="round"/><circle cx="12" cy="17" r="1.3" fill="' + iconInk + '"/></svg>'
        String statusIcon = isPass
            ? "<!--[if mso]>&#10003;<![endif]--><!--[if !mso]><!-->${fluentCheckmarkSvg}<!--<![endif]-->"
            : (isWarn
                ? "<!--[if mso]>!<![endif]--><!--[if !mso]><!-->${fluentWarningSvg}<!--<![endif]-->"
                : "<!--[if mso]>&#10007;<![endif]--><!--[if !mso]><!-->${fluentDismissSvg}<!--<![endif]-->")

        String monoStack = "'JetBrains Mono', 'Courier New', ui-monospace, monospace"
        String sansStack = "'Inter', 'Segoe UI', Tahoma, Arial, sans-serif"

        // ── Pass-rate bar + legend — table/div based, no SVG donut, so it
        //    survives clients that strip inline SVG. ──
        String donutBlock = ''
        if (hasTestCounts) {
            int totalN = (data.totalTests as Integer) ?: 0
            int passedN = (data.passedTests as Integer) ?: 0
            int failedN = (data.failedTests as Integer) ?: 0
            int skippedN = (data.skippedTests as Integer) ?: 0
            int passRate = totalN > 0 ? Math.round((passedN / (double) totalN) * 100) as int : 0
            String rateColor = passRate == 100 ? pass : (passRate >= 90 ? warn : fail)

            int wPass = totalN > 0 ? Math.round((passedN / (double) totalN) * 100) as int : 0
            int wFail = totalN > 0 ? Math.round((failedN / (double) totalN) * 100) as int : 0
            int wSkip = Math.max(0, 100 - wPass - wFail)

            donutBlock = """
            <tr>
              <td class="email-pad" style="padding:20px 24px 0 24px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border:1px solid ${border};border-radius:12px;">
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
                          <td style="height:10px;line-height:10px;font-size:0;border-radius:4px;overflow:hidden;background:${border};">${wPass > 0 ? "<span style=\"display:inline-block;height:10px;width:${wPass}%;background:${pass};\">&nbsp;</span>" : ''}${wFail > 0 ? "<span style=\"display:inline-block;height:10px;width:${wFail}%;background:${fail};\">&nbsp;</span>" : ''}${wSkip > 0 ? "<span style=\"display:inline-block;height:10px;width:${wSkip}%;background:${skip};\">&nbsp;</span>" : ''}</td>
                        </tr>
                      </table>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin-top:14px;">
                        <tr>
                          <td width="33%" style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${pass};margin-right:6px;"></span>Passed
                            <span style="font-family:${monoStack};font-weight:700;float:right;">${passedN}</span>
                          </td>
                        </tr>
                        <tr><td style="height:6px;font-size:0;">&nbsp;</td></tr>
                        <tr>
                          <td style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${fail};margin-right:6px;"></span>Failed
                            <span style="font-family:${monoStack};font-weight:700;float:right;">${failedN}</span>
                          </td>
                        </tr>
                        <tr><td style="height:6px;font-size:0;">&nbsp;</td></tr>
                        <tr>
                          <td style="font-size:12px;color:${textMuted};">
                            <span style="display:inline-block;width:7px;height:7px;border-radius:50%;background:${skip};margin-right:6px;"></span>Skipped
                            <span style="font-family:${monoStack};font-weight:700;float:right;">${skippedN}</span>
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

        // ── Stat cards (Total / Passed / Failed / Skipped). ──
        String testSummaryBlock = ''
        if (hasTestCounts) {
            List<Map> stats = [
                [label: 'Total',   value: data.totalTests,   color: textMuted, bgc: 'transparent', bord: border],
                [label: 'Passed',  value: data.passedTests,  color: pass,      bgc: passSoft,        bord: passBorder],
                [label: 'Failed',  value: data.failedTests,  color: fail,      bgc: failSoft,        bord: failBorder],
                [label: 'Skipped', value: data.skippedTests, color: warn,      bgc: warnSoft,        bord: warnBorder],
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

        // ── Meta strip: Job / Branch / Build / Started / Duration — mono values,
        //    dim uppercase labels. Rendered as a proper 2-column grid: rows are
        //    built explicitly from chunks of 2, instead of stuffing every cell
        //    into one <tr> at width:50% (which is what broke this section
        //    before — 5 cells × 50% in a single row overflows to 250%). ──
        String jobNameDisplay = data.jobName ?: (env.JOB_NAME ?: '—')
        List<List<String>> metaRows = [
            ['Job', "${jobNameDisplay}"],
            ['Branch', "${env.BRANCH_NAME ?: '—'}"],
            ['Build', "#${env.BUILD_NUMBER ?: '—'}"],
            ['Started', "${data.startTime}"],
            ['Duration', "${currentBuild.durationString}"],
        ]
        List<String> metaRowsHtml = []
        metaRows.collate(2).each { pair ->
            String cell1 = """
              <td class="meta-cell" width="50%" valign="top" style="padding:10px 12px;border:1px solid ${border};">
                <div style="font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:${textDim};">${pair[0][0]}</div>
                <div style="font-family:${monoStack};font-size:12px;margin-top:4px;word-break:break-word;">${pair[0][1]}</div>
              </td>
            """
            String cell2 = pair.size() > 1 ? """
              <td class="meta-cell" width="50%" valign="top" style="padding:10px 12px;border:1px solid ${border};">
                <div style="font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:${textDim};">${pair[1][0]}</div>
                <div style="font-family:${monoStack};font-size:12px;margin-top:4px;word-break:break-word;">${pair[1][1]}</div>
              </td>
            ''' : """<td class="meta-cell" width="50%" style="border:none;">&nbsp;</td>"""
            metaRowsHtml << "<tr>${cell1}${cell2}</tr>"
        }
        String metaStripBlock = metaRowsHtml.join('')

        // ── Quick links — solid accent button for the primary link, outlined
        //    (no fill) pills for the rest, so nothing but the deliberate CTA
        //    carries a background color. ──
        List<Map> links = [
            [label: 'Build Information', url: env.BUILD_URL, primary: true],
            [label: 'Pipeline Overview', url: "${env.BUILD_URL}pipeline-overview", primary: false],
            [label: 'Console Output', url: "${env.BUILD_URL}console", primary: false],
        ]
        if (showTests) {
            links << [label: 'Test Results', url: "${env.BUILD_URL}testReport", primary: false]
        }
        String linksBlock = links.collect { link ->
            String bgc = link.primary ? accent : 'transparent'
            String fg = link.primary ? '#ffffff' : ''
            String bd = link.primary ? accent : borderStrong
            String colorStyle = fg ? "color:${fg};" : ''
            '''
            <tr>
              <td style="padding:0 0 8px 0;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td align="center" bgcolor="${bgc}" style="border-radius:7px;border:1px solid ${bd};">
                      <a href="${link.url}" target="_blank" style="display:block;padding:11px 16px;font-size:13px;font-weight:600;${colorStyle}text-decoration:none;font-family:${sansStack};">${link.label}</a>
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
        <body style="margin:0;padding:0;font-family:${sansStack};">
          <div style="display:none;max-height:0;overflow:hidden;opacity:0;">${statusLabel} · ${currentBuild.fullDisplayName} · ${currentBuild.durationString}</div>
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td align="center" style="padding:28px 12px;">
                <table role="presentation" class="email-wrap" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;border-radius:14px;overflow:hidden;border:1px solid ${border};">

                  <!-- Brand row -->
                  <tr>
                    <td class="email-pad" style="padding:22px 24px 16px 24px;border-bottom:1px solid ${border};">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                          <td width="38" valign="middle">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td width="34" height="34" align="center" valign="middle" bgcolor="${accent}" style="border-radius:8px;font-family:${sansStack};font-size:15px;font-weight:700;color:#ffffff;">CI</td>
                              </tr>
                            </table>
                          </td>
                          <td valign="middle" style="padding-left:12px;">
                            <div style="font-size:14px;font-weight:700;letter-spacing:-.01em;">Jenkins CI</div>
                            <div style="font-size:10px;font-weight:600;letter-spacing:.08em;text-transform:uppercase;color:${textDim};margin-top:1px;">Test Report</div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Status pill (no divider — label + meta flow in one line) -->
                  <tr>
                    <td class="email-pad" style="padding:20px 24px 0 24px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="background:${statusSoft};border:1px solid ${statusBorder};border-radius:999px;">
                        <tr>
                          <td style="padding:8px 16px 8px 10px;">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td width="22" height="22" align="center" valign="middle" bgcolor="${statusColor}" style="border-radius:50%;">${statusIcon}</td>
                                <td style="padding-left:9px;font-size:13px;font-weight:700;color:${statusColor};white-space:nowrap;">
                                  ${statusLabel}${hasTestCounts ? "<span style=\"font-weight:500;color:${textMuted};font-family:${monoStack};font-size:11px;\">&nbsp;&nbsp;&#183;&nbsp;&nbsp;${data.passedTests}/${data.totalTests} passed&nbsp;&nbsp;&#183;&nbsp;&nbsp;${currentBuild.durationString}</span>" : ''}
                                </td>
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
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border-collapse:separate;border-spacing:0 6px;">
                        ${metaStripBlock}
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
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border:1px solid ${border};border-radius:8px;">
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

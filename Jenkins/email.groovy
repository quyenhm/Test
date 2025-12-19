
def sendEmail(Map data [:]) {
    def showTests = data.get('showTests', true)

  if (data.email) {
    def body = """
    <!DOCTYPE html>
    <html>
    <body style="font-family: 'Segoe UI', sans-serif; font-size:14px; color:#333;">

    <h2 style="color:${data.color};">${data.title}</h2>

    <table cellpadding="6" cellspacing="0">
      <tr><td><b>Started</b></td><td>${data.startTime}</td></tr>
      <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
      <tr><td><b>Result</b></td><td><b style="color:${data.color};">${data.result}</b></td></tr>
    </table>

    <h3>Summary</h3>
    <ul>
      <li><b>Job:</b> ${data.jobName}</li>
      <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
    </ul>

    <h3>Quick Links</h3>
    <ul>
      <li><a href="${env.BUILD_URL}">Build</a></li>
      <li><a href="${env.BUILD_URL}console">Console</a></li>
      ${showTests ? "<li><a href='${env.BUILD_URL}testReport'>Tests</a></li>" : ""}
    </ul>

    <p>${data.message}</p>

    <p style="font-size:12px;color:#777;">
    Regards,<br/>Jenkins CI
    </p>

    </body>
    </html>
    """.stripIndent().trim()

    mail(
          to: data.email,
          subject: "${data.title}: ${currentBuild.fullDisplayName}",
          mimeType: 'text/html',
          body: body
      )
  }
  else{
                    echo 'No notification email configured ($ctx.email is missing or empty).'

  }
}

return this

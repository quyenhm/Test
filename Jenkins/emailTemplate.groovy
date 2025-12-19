def build(Map ctx, Map opt = [:]) {
    def title     = opt.title
    def result    = opt.result
    def color     = opt.color
    def message   = opt.message
    def showTests = opt.get('showTests', true)

    return """
    <!DOCTYPE html>
    <html>
    <body style="font-family: 'Segoe UI', sans-serif; font-size:14px; color:#333;">

    <h2 style="color:${color};">${title}</h2>

    <table cellpadding="6" cellspacing="0">
      <tr><td><b>Started</b></td><td>${ctx.startTime}</td></tr>
      <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
      <tr><td><b>Result</b></td><td><b style="color:${color};">${result}</b></td></tr>
    </table>

    <h3>Summary</h3>
    <ul>
      <li><b>Job:</b> ${ctx.jobName}</li>
      <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
    </ul>

    <h3>Quick Links</h3>
    <ul>
      <li><a href="${env.BUILD_URL}">Build</a></li>
      <li><a href="${env.BUILD_URL}console">Console</a></li>
      ${showTests ? "<li><a href='${env.BUILD_URL}testReport'>Tests</a></li>" : ""}
    </ul>

    <p>${message}</p>

    <p style="font-size:12px;color:#777;">
    Regards,<br/>Jenkins CI
    </p>

    </body>
    </html>
    """.stripIndent().trim()
}

def hello(Map data = [:]) {
    return "Hello ${data.name} - ${data.age}"
}

def hello2(Map data = [:]) {
    return "Hello " + data.name + " -- " + data.age
}

return this

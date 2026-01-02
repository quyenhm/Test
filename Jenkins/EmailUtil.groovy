
void sendEmail(Map data = [:]) {
    String showTests = data.get('showTests', true) ? 'normal' : 'none'

    if (data.email) {
        String body = """
        <!DOCTYPE html>
        <html>
        <body style="font-family: 'Mulish', 'Segoe UI', Lato, Tahoma, sans-serif; color:#333;">
            <h2 style="color:${data.color};"><b>${data.result}</b></h2>
            <hr>
            <table>
                <tr>
                    <td><b>Started</b></td>
                    <td>:</td>
                    <td>${data.startTime}</td>
                </tr>
                <tr>
                    <td><b>Duration</b></td>
                    <td>:</td>
                    <td>${currentBuild.durationString}</td>
                </tr>
            </table>
            <hr>
            <h3>Summary</h3>
            <ul>
                <li><b>Job:</b> ${env.JOB_NAME.tokenize('/').dropRight(1).last()}</li>
                <li><b>Branch:</b> ${env.BRANCH_NAME}</li>
                <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
            </ul>
            <h3>Quick Links</h3>
            <ul>
                <li><a href="${env.BUILD_URL}">Build Information</a></li>
                <li><a href="${env.BUILD_URL}pipeline-overview">Pipeline Overview</a></li>
                <li><a href="${env.BUILD_URL}console">Console Output</a></li>
                <li style="display: ${showTests};"><a href='${env.BUILD_URL}testReport'>Test Results</a></li>
            </ul>
            <p>${data.message}</p>
            <p style="color:#777;">
                Regards,<br />
                Jenkins CI
            </p>
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
            echo "Failed to send email to ${data.email}: ${err}"
        }
    }
    else {
        echo 'No notification email configured ($ctx.email is missing or empty).'
    }
}

return this

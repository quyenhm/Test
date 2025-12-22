void removeEmoji(String s) {
    s.replaceAll(/[\p{So}\p{Cn}\p{Sk}\p{Cs}]/, '')
}

void sendEmail(Map data = [:]) {
    String showTests = data.get('showTests', true) ? 'normal' : 'none'

    if (data.email) {
        String body = """
        <!DOCTYPE html>
        <html>
        <body style="font-family: 'Mulish', 'Segoe UI', Lato, Tahoma, sans-serif; color:#333;">
            <h2 style="color:${data.color};"><b>${removeEmoji(data.title)}</b></h2>
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
            <h3>Summary</h3>
            <ul>
                <li><b>Job:</b> ${env.JOB_NAME.tokenize('/').dropRight(1).last()}</li>
                <li><b>Branch:</b> ${env.BRANCH_NAME}</li>
                <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
            </ul>
            <h3>Quick Links</h3>
            <ul>
                <li><a href="${env.BUILD_URL}">Build Information</a></li>
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

        mail(
            to: data.email,
            subject: "${data.title}: ${currentBuild.fullDisplayName}",
            mimeType: 'text/html',
            body: body
        )
    }
    else {
        echo 'No notification email configured ($ctx.email is missing or empty).'
    }
}

return this

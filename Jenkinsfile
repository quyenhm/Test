pipeline {
    agent any

    environment {
        SqlServer = credentials('SQL')
    }

    stages {
        stage('Build') {
            steps {
                echo 'Building app'
                pwsh '& ".\\ii.ps1" -Build'
            }
        }

        stage('Test') {
            steps {
                echo 'Running integration tests...'
                pwsh '& ".\\ii.ps1" -EditConn -Username $env:SqlServer_USR -Password $env:SqlServer_PSW'
                pwsh '& ".\\ii.ps1" -Test'

                mstest(
                    testResultsFile: 'Tests/TestResults/**/*.trx',
                    failOnError: true
                )
            }
        }

        stage('Publish') {
            when {
                branch comparator: 'EQUALS', pattern: 'main'
            }
            steps {
                echo 'Publishing the CLI...'
                pwsh '& ".\\ii.ps1" -Publish'

                archiveArtifacts(
                    artifacts: 'Delivery.Cli\\bin\\ifsintall_v*.zip',
                    fingerprint: true,
                    onlyIfSuccessful: true,
                    allowEmptyArchive: true
                )
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully ✅'
        }
        failure {
            echo 'Pipeline failed ❌'
            script {
                if (env.IFSINSTALL_NOTIFY_EMAIL?.trim()) {
                    mail(
                        to: env.IFSINSTALL_NOTIFY_EMAIL,
                        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} ❌",
                        body: "Check log: ${env.BUILD_URL}"
                    )
                } else {
                    echo 'No notification email configured ($env.IFSINSTALL_NOTIFY_EMAIL is missing or empty).'
                }
            }
        }
        always {
            echo 'Cleaning up workspace...'
            pwsh '& ".\\ii.ps1" -RemoveBin -RemoveTestUser -ResetConn'
        }
    }
}

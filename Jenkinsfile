pipeline {
    agent any

    environment {
        SqlServer = credentials('SQL')
    }

    parameters {
        string(name: 'NOTIFY_EMAIL', description: 'Email to notify')
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

                mstest(testResultsFile: 'Tests/TestResults/**/*.trx', failOnError: true)
            }
        }

        stage('Publish') {
            when {
                branch comparator: 'EQUALS', pattern: 'main'
            }
            steps {
                echo 'Publishing the CLI...'
                pwsh '& ".\\ii.ps1" -Publish'

                archiveArtifacts(artifacts: 'Delivery.Cli\\bin\\ifsintall_v*.zip', fingerprint: true)
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully ✅'
        }
        failure {
            echo 'Pipeline failed ❌'
            mail to: "${params.NOTIFY_EMAIL}",
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} ❌",
                body: "Check log: ${env.BUILD_URL}"
        }
        always {
            echo 'Cleaning up workspace...'
            pwsh '& ".\\ii.ps1" -RemoveBin -RemoveTestUser -ResetConn'
        }
    }
}

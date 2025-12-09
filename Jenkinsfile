pipeline {
    agent any

    parameters {
        booleanParam defaultValue: false, name: 'Run Tests in Parallel?'
        booleanParam defaultValue: false, name: 'Test Pwsh throw'
        string defaultValue: '0', name: 'Exit Code for Tests'
    }

    stages {

        stage('Init') {
            steps {
                script {
                    env.STARTED_AT = new Date(currentBuild.startTimeInMillis ?: System.currentTimeMillis()).toString()
                    env.FORMATTED_DATE = new Date().format("yyyy.MM.dd_HH.mm")
                    env.TEST_OUTPUT = "Tests/TestResults/${env.FORMATTED_DATE}"
                    env.NAME = "${env.JOB_NAME.replace('%2F', '/')} | #${env.BUILD_NUMBER}"
                }

                echo "1: $env.JOB_NAME"
                echo "2: $env.NAME"
                echo "3: ${env.NAME}"
                echo """
                4: $env.NAME
                """

            }
        }

        stage('Build') {
            steps {
                echo 'Building app'
                pwsh '& ./ii.ps1 -Build'
            }
        }

        stage('Test') {
            steps {
                script {
                    if (params['Run Tests in Parallel?']) {
                        echo 'Running tests in parallel...'
                        def projects = ['CORE', 'AML', 'SAFE', 'BW', 'IB', 'DIGI', 'SYS', 'CLI', 'LIC']
                        def parallelStages = [:]

                        projects.each { proj ->
                            parallelStages[proj] = {
                                pwsh '& ./ii.ps1 -Test -Projects ' + proj
                            }
                        }

                        parallel(parallelStages)
                    } else {
                        echo 'Running tests sequentially...'
                        echo 'Running integration tests...'
                        pwsh '& ./ii.ps1 -Test'
                        echo 'Running unit tests...'
                        pwsh '& ./ii.ps1 -Test -ExitCode ' + params['Exit Code for Tests']

                        if (params['Test Pwsh throw']) {
                            echo 'Running Pwsh throw test...'
                            pwsh '& ./ii.ps1 -Throw'
                        }
                    }
                }
            }
        }

        stage('Publish') {
            when {
                branch comparator: 'EQUALS', pattern: 'main'
            }
            steps {
                echo 'Publishing the CLI...'
                pwsh '& ./ii.ps1 -Publish'

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
                    // mail(
                    //     to: env.IFSINSTALL_NOTIFY_EMAIL,
                    //     subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} ❌",
                    //     body: "Check log: ${env.BUILD_URL}"
                    // )
                    echo 'Emailed.'
                } else {
                    echo 'No notification email configured ($env.IFSINSTALL_NOTIFY_EMAIL is missing or empty).'
                }
            }
        }
        always {
            echo 'Cleaning up workspace...'
        }
    }
}

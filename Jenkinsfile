def emailTpl = libraryResource 'Jenkins/emailTemplate.groovy'

def ctx = [
    email : null,
    jobName: null,
    testOutput: null,
    startTime: null,
]

pipeline {
    agent any

    environment {
        SqlServer = credentials('SQL_SERVER')
    }

    options {
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam defaultValue: false, name: 'Run Tests in Parallel?'
        booleanParam defaultValue: false, name: 'Test Pwsh throw'
        string defaultValue: '0', name: 'Exit Code for Tests'
    }

    stages {

        stage('Init') {
            steps {
                script {
                    def dateFormat = new Date().format("yyyy.MM.dd_HH.mm")

                    ctx.email = env.IFSINSTALL_NOTIFY_EMAIL?.trim()
                    ctx.jobName = env.JOB_NAME.replace('%2F', '/')
                    ctx.testOutput = "./Tests/TestResults/${dateFormat}"
                    ctx.startTime = new Date(currentBuild.startTimeInMillis)
                }
            }
        }

        stage('Config') {
            steps {
                echo 'Update credential in connection string...'
                pwsh '& ./ii.ps1 -EditConn -Username $env:SqlServer_USR -Password $env:SqlServer_PSW'
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
                                pwsh "& ./ii.ps1 -Test -TestNoBuild -Projects '${proj}' -TestOutput '${ctx.testOutput}'"
                            }
                        }

                        parallel(parallelStages)
                    } else {
                        echo 'Running integration tests...'
                        pwsh "& ./ii.ps1 -Test -TestNoBuild -TestOutput '${ctx.testOutput}'"

                        pwsh '& ./ii.ps1 -Test -ExitCode ' + params['Exit Code for Tests']

                        if (params['Test Pwsh throw']) {
                            echo 'Running Pwsh throw test...'
                            pwsh '& ./ii.ps1 -Throw'
                        }
                    }
                }
            }
        }

        stage('Report') {
            steps {
                echo 'Collecting test results...'

                // mstest(
                //     testResultsFile: "${ctx.testOutput}/*.trx",
                //     failOnError: true
                // )
            }
        }

        stage('Verify') {
            steps {
                echo 'Verifying test results...'

                script {
                    def result = currentBuild.result ?: 'SUCCESS'
                    echo "Build result after mstest: ${result}"

                    if (ctx.email && result == 'UNSTABLE') {
                        mail(
                            to: ctx.email,
                            subject: "⚠️ TEST FAILED: ${currentBuild.fullDisplayName}",
                            mimeType: 'text/html',
                            body: emailTpl.build(
                                ctx,
                                "⚠️ TEST UNSTABLE",
                                "UNSTABLE",
                                "#f39c12",
                                "Some tests are unstable or flaky. Please review the test results and take appropriate action."
                            )
                        )
                    }
                }
            }
        }

        stage('Publish') {
            when {
                branch comparator: 'EQUALS', pattern: 'master'
            }
            steps {
                echo 'Publishing the CLI...'
                pwsh '& ./ii.ps1 -Publish -NoPrompt'

                archiveArtifacts(
                    artifacts: '**/Delivery.Cli/bin/ifsinstall_v*.zip',
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

            script {
                def prevResult = currentBuild.previousBuild?.result
                def currResult = currentBuild.currentResult

                if (ctx.email &&prevResult != 'SUCCESS' && currResult == 'SUCCESS') {
                    mail(
                        to: ctx.email,
                        subject: "✅ BACK TO STABLE: ${currentBuild.fullDisplayName}",
                        mimeType: 'text/html',
                        body: emailTpl.build(
                            ctx,
                            "✅ TEST PASSED",
                            "SUCCESS",
                            "#27ae60",
                            "The issues causing previous test failures have been resolved. The build is now stable."
                        )
                    )
                }
            }

            echo '🧹 Cleaning up workspace...'
            pwsh '& ./ii.ps1 -RemoveBin -RemoveTestUser -ResetConn'
        }
        failure {
            echo 'Pipeline failed ❌'
            script {
                if (ctx.email) {
                    mail(
                        to: ctx.email,
                        subject: "❌ BUILD FAILED: ${currentBuild.fullDisplayName} - Immediate Action Required",
                        mimeType: 'text/html',
                        body: emailTpl.build(
                            ctx,
                            "❌ BUILD FAILURE",
                            "FAILED",
                            "#c0392b",
                            "Please investigate the failure as soon as possible to maintain the integrity of the build process.",
                            false
                        )
                    )
                } else {
                    echo 'No notification email configured ($ctx.email is missing or empty).'
                }
            }
        }
        always {
            echo 'Pipeline finished 🏁'
        }
    }
}

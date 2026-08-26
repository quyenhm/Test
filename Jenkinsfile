/* groovylint-disable LineLength, NestedBlockDepth */

def emailUtil

Map ctx = [
    email: env.IFSINSTALL_NOTIFY_EMAIL?.trim(),
    jobName: env.JOB_NAME.replace('%2F', '/'),
    testOutput: 'Tests/TestResults/' + new Date().format('yyyy.MM.dd_HH.mm'),
    startTime: new Date(currentBuild.startTimeInMillis),
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
        booleanParam(name: 'RunTestsInParallel', defaultValue: false, description: 'Run Tests in Parallel?')
        booleanParam(name: 'TestPwshThrow', defaultValue: false, description: 'Test Pwsh throw')
        string(name: 'ExitCodeForTests', defaultValue: '0', description: 'Exit code for tests step')
    }

    stages {
        stage('Init') {
            steps {
                script {
                    emailUtil = load 'Jenkins/EmailUtil.groovy'
                    echo "${ctx.startTime}"
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
                    if (params.RunTestsInParallel) {
                        echo 'Running tests in parallel...'
                        String[] projects = ['CORE', 'AML', 'SAFE', 'BW', 'IB', 'DIGI', 'SYS', 'CLI', 'LIC']
                        Map parallelStages = [:]

                        projects.each { proj ->
                            parallelStages[proj] = {
                                pwsh "& ./ii.ps1 -Test -TestNoBuild -Projects '${proj}' -TestOutput '${ctx.testOutput}'"
                            }
                        }

                        parallel(parallelStages)
                    } else {
                        echo 'Running integration tests...'
                        pwsh "& ./ii.ps1 -Test -TestNoBuild -TestOutput '${ctx.testOutput}'"

                        pwsh '& ./ii.ps1 -Test -ExitCode ' + params.ExitCodeForTests

                        if (params.TestPwshThrow) {
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

                mstest(
                    testResultsFile: '*.trx',
                    failOnError: true
                )

                script {
                    def trxFiles = findFiles(glob: "*.trx")

                    if (trxFiles) {
                        int total = 0
                        int passed = 0
                        int failed = 0
                        int skipped = 0

                        trxFiles.each { f ->
                            def xml = new XmlSlurper().parseText(readFile(f.path))
                            def counters = xml.ResultSummary.Counters

                            total += counters.@total.toInteger()
                            passed += counters.@passed.toInteger()
                            failed += counters.@failed.toInteger() + counters.@error.toInteger()
                            skipped += counters.@notExecuted.toInteger() + counters.@inconclusive.toInteger()
                        }

                        ctx.totalTests = total
                        ctx.passedTests = passed
                        ctx.failedTests = failed
                        ctx.skippedTests = skipped

                        echo 'Test results:'
                        echo "  Total:   ${ctx.totalTests}"
                        echo "  Passed:  ${ctx.passedTests}"
                        echo "  Failed:  ${ctx.failedTests}"
                        echo "  Skipped: ${ctx.skippedTests}"
                    } else {
                        echo 'No .trx files found — unable to compute test counts.'
                    }
                }
            }
        }

        stage('Verify') {
            steps {
                echo 'Verifying test results...'

                script {
                    String result = currentBuild.result ?: 'SUCCESS'
                    echo "Build result after mstest: ${result}"

                    if (result == 'UNSTABLE') {
                        emailUtil.sendEmail([
                            email: ctx.email,
                            title: '⚠️ TEST UNSTABLE',
                            result: 'TEST UNSTABLE',
                            jobName: ctx.jobName,
                            startTime: ctx.startTime,
                            color: '#f5ba45',
                            message: 'Please review the test failures and take corrective action.'
                        ])
                    }
                }
            }
        }

        stage('Publish') {
            when {
                branch comparator: 'EQUALS', pattern: 'main'
            }
            steps {
                echo 'Fetching git tags...'
                pwsh 'git fetch --tags'

                echo 'Publishing the CLI...'
                pwsh '& ./ii.ps1 -Publish -NoPrompt'

                archiveArtifacts(
                    artifacts: '**/file_v*.txt',
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
                String prevResult = currentBuild.previousBuild?.result
                String currResult = currentBuild.currentResult

                if (prevResult != 'SUCCESS' && currResult == 'SUCCESS') {
                    emailUtil.sendEmail([
                        email: ctx.email,
                        title: '✅ BACK TO STABLE',
                        result: 'TEST PASSED',
                        jobName: ctx.jobName,
                        startTime: ctx.startTime,
                        color: '#8ac054',
                        message: 'The issues causing previous test failures have been resolved. The build is now stable.'
                    ])
                }
            }

            echo '🧹 Cleaning up workspace...'
            pwsh '& ./ii.ps1 -RemoveBin -RemoveTestUser -ResetConn'
        }
        failure {
            echo 'Pipeline failed ❌'
            script {
                emailUtil.sendEmail([
                    email: ctx.email,
                    title: '❌ BUILD FAILED',
                    result: 'BUILD FAILED',
                    jobName: ctx.jobName,
                    startTime: ctx.startTime,
                    color: '#e8563f',
                    message: 'Please investigate the failure as soon as possible to maintain the integrity of the build process.',
                    showTests: false
                ])
            }
        }
        always {
            echo 'Pipeline finished 🏁'
        }
    }
}

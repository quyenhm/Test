/* groovylint-disable LineLength, NestedBlockDepth, NoJavaUtilDate */

def emailUtil
def cleanupCatchOptions = [buildResult: 'SUCCESS', stageResult: 'UNSTABLE']

Map ctx = [
    email: env.IFSINSTALL_NOTIFY_EMAIL?.trim(),
    jobName: env.JOB_NAME.replace('%2F', '/'),
    testOutput: 'Tests/TestResults/2026.01.01_00.00',
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
        booleanParam(name: 'RunTestsInParallel', defaultValue: true, description: 'Run Tests in Parallel?')
    }

    stages {
        stage('Init') {
            steps {
                script {
                    echo 'Force-apply .gitattributes (destructive, discards changes)'
                    pwsh 'git rm --cached -r . && git reset --hard'

                    emailUtil = load 'Jenkins/EmailUtil.groovy'
                }
            }
        }

        stage('Config') {
            steps {
                echo 'Update credential in connection string...'
                pwsh '& ./ii.ps1 -EditConn -SqlInfo $env:SqlServer_PSW'
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
                                pwsh "& ./ii.ps1 -Test -SkipBuild -NoReport -Projects '${proj}' -OutputPath '${ctx.testOutput}'"
                            }
                        }

                        parallel(parallelStages)
                    } else {
                        echo 'Running integration tests...'
                        pwsh "& ./ii.ps1 -Test -SkipBuild -NoReport -OutputPath '${ctx.testOutput}'"
                    }
                }
            }
        }

        stage('Report') {
            steps {
                echo 'Collecting test results...'

                mstest(
                    testResultsFile: "${ctx.testOutput}/*.trx",
                    failOnError: true
                )

                script {
                    String summaryRaw = pwsh(
                        script: "& ./ii.ps1 -Summary -OutputPath '${ctx.testOutput}'",
                        returnStdout: true
                    ).trim()

                    List<String> parts = summaryRaw.tokenize(',')

                    if (parts.size() == 4) {
                        ctx.totalTests = parts[0].toInteger()
                        ctx.passedTests = parts[1].toInteger()
                        ctx.failedTests = parts[2].toInteger()
                        ctx.skippedTests = parts[3].toInteger()

                        echo 'Test results:'
                        echo "  Total:   ${ctx.totalTests}"
                        echo "  Passed:  ${ctx.passedTests}"
                        echo "  Failed:  ${ctx.failedTests}"
                        echo "  Skipped: ${ctx.skippedTests}"
                    } else {
                        echo "Unable to parse test counts from ii.ps1 output: '${summaryRaw}'"
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
                            message: 'Please review the test failures and take corrective action.',
                            totalTests: ctx.totalTests,
                            passedTests: ctx.passedTests,
                            failedTests: ctx.failedTests,
                            skippedTests: ctx.skippedTests
                        ])
                    }
                }
            }
        }

        stage('Publish') {
            when {
                anyOf {
                    branch comparator: 'EQUALS', pattern: 'master'
                    branch comparator: 'REGEXP', pattern: '(release|hotfix)/.*'
                }
            }
            steps {
                echo 'Fetching git tags...'
                pwsh 'git fetch --tags --force'

                echo 'Publishing the CLI...'
                pwsh '& ./ii.ps1 -Publish'

                script {
                    if (env.BRANCH_NAME == 'master') {
                        archiveArtifacts(
                            artifacts: '**/Delivery.Cli/bin/ifsinstall_v*.zip',
                            fingerprint: true,
                            onlyIfSuccessful: true,
                            allowEmptyArchive: true
                        )
                    }
                }
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
                        color: '#4caf50',
                        message: 'The issues causing previous test failures have been resolved. The build is now stable.',
                        totalTests: ctx.totalTests,
                        passedTests: ctx.passedTests,
                        failedTests: ctx.failedTests,
                        skippedTests: ctx.skippedTests
                    ])
                }
            }
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
                    showTests: false,
                    totalTests: ctx.totalTests,
                    passedTests: ctx.passedTests,
                    failedTests: ctx.failedTests,
                    skippedTests: ctx.skippedTests
                ])
            }
        }
        always {
            echo '🧹 Cleaning up workspace...'

            catchError(cleanupCatchOptions) {
                pwsh '& ./ii.ps1 -RemoveTestUser'
            }

            catchError(cleanupCatchOptions) {
                pwsh '& ./ii.ps1 -RemoveBin -ResetConn'
            }

            echo 'Pipeline finished 🏁'
        }
    }
}

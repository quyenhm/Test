/* groovylint-disable-next-line CompileStatic */
properties([
    parameters([
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run tests before deploy?')
    ])
])

node {
    version = "1.0.${env.BUILD_NUMBER}"

    try {
        stage('Build') {
            echo "Building app version ${version}"
        }

        stage('Test') {
            if (params.RUN_TESTS) {
                parallel(
                    'Unit Tests': {
                        echo 'Running unit tests...'
                    },
                    'Integration Tests': {
                        echo 'Running integration tests...'

                        withCredentials(
                            [usernamePassword(
                                credentialsId: 'SQL',
                                usernameVariable: 'USR',
                                passwordVariable: 'PSW')]
                        ) {
                            pwsh '''
                                & "$env:WORKSPACE\\ii.ps1" -EditConn -Username $env:USR -Password $env:PSW
                            '''
                        }
                    }
                )
            } else {
                echo 'Skipping tests'
            }
        }

        stage('Deploy') {
            echo "Deploying version ${version}"
        }

        echo 'Pipeline completed successfully ✅'
    }
    catch (err) {
        echo "Pipeline failed ❌: ${err}"
        currentBuild.result = 'FAILURE'
        throw err
    }
    finally {
        echo 'Cleaning up workspace...'
    }
}

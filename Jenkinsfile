/* groovylint-disable-next-line CompileStatic */
properties([
    parameters([
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run tests before deploy?')
    ])
])

runIntegrationTests {
    withCredentials(
        [usernamePassword(
            credentialsId: 'SQL',
            passwordVariable: 'SQL_PWD',
            usernameVariable: 'SQL_USER')]
    ) {
        pwsh """
            & "${env.WORKSPACE}\\ii.ps1" -EditConn -Username "${env.SQL_USER}" -Password "${env.SQL_PWD}"
        """
    }
}

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
                        runIntegrationTests()
                    }
                )
            } else {
                echo 'Skipping tests'
            }
        }

        stage('Deploy') {
            echo "Deploying version ${VERSION} to ${APP_ENV}"
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

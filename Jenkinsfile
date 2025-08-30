
node {
    try {
        stage('Build') {
            echo "Building app version ${version}"
        }

        stage('Test') {
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

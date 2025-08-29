node {
    def APP_ENV = "staging"
    def VERSION = "1.0.${env.BUILD_NUMBER}"

    try {
        stage('Build') {
            echo "Building app version ${VERSION}"
        }

        stage('Test') {
            if (params.RUN_TESTS) {
                parallel(
                    "Unit Tests": {
                        echo "Running unit tests..."
                    },
                    "Integration Tests": {
                        echo "Running integration tests..."
                    }
                )
            } else {
                echo "Skipping tests"
            }
        }

        stage('Deploy') {
            echo "Deploying version ${VERSION} to ${APP_ENV}"
        }

        echo "Pipeline completed successfully ✅"
    }
    catch (err) {
        echo "Pipeline failed ❌: ${err}"
        currentBuild.result = "FAILURE"
        throw err
    }
    finally {
        echo "Cleaning up workspace..."
    }
}

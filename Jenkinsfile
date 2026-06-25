pipeline {
    agent any

    parameters {
        choice(
            name        : 'SUITE',
            choices     : ['smoke', 'regression', 'full'],
            description : 'TestNG suite to run (smoke = 6 critical tests, regression = all 20, full = all parallel)'
        )
    }

    // Add Docker and common tool paths for macOS Jenkins agents
    environment {
        PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
    }

    stages {

        // ── 1. Pull source from GitHub ────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ── 2. Inject credentials from Jenkins Credential Store ───────────────
        // Requires two Secret Text credentials with IDs: APP_EMAIL, APP_PASSWORD
        // Uses macOS-compatible sed -i '' syntax
        stage('Inject Credentials') {
            steps {
                withCredentials([
                    string(credentialsId: 'APP_EMAIL',    variable: 'APP_EMAIL'),
                    string(credentialsId: 'APP_PASSWORD', variable: 'APP_PASSWORD')
                ]) {
                    sh '''
                        cp src/test/resources/config.properties.template \
                           src/test/resources/config.properties
                        sed -i '' "s/YOUR_EMAIL_HERE/${APP_EMAIL}/g"       src/test/resources/config.properties
                        sed -i '' "s/YOUR_PASSWORD_HERE/${APP_PASSWORD}/g" src/test/resources/config.properties
                        sed -i '' "s/YOUR_EMAIL_HERE/${APP_EMAIL}/g"       src/test/resources/testdata/auth-data.json
                        sed -i '' "s/YOUR_PASSWORD_HERE/${APP_PASSWORD}/g" src/test/resources/testdata/auth-data.json
                    '''
                }
            }
        }

        // ── 3. Run tests inside Docker (Selenium Grid + Maven) ────────────────
        // Uses ARM64 Chromium image — matches local Mac M-series hardware.
        // docker-compose.ci.yml is only needed for Linux/AMD64 Jenkins agents.
        stage('Run Tests') {
            steps {
                sh """
                    SUITE=${params.SUITE} \
                    docker compose \
                        -f docker-compose.yml \
                        up --build --abort-on-container-exit --exit-code-from tests
                """
            }
        }
    }

    // ── Post: always publish Allure report and tear down containers ───────────
    post {
        always {
            allure([
                includeProperties: false,
                jdk              : '',
                properties       : [],
                reportBuildPolicy: 'ALWAYS',
                results          : [[path: 'target/allure-results']]
            ])
            sh 'docker compose -f docker-compose.yml down -v --remove-orphans || true'
        }
        success {
            echo "All ${params.SUITE} tests passed."
        }
        failure {
            echo "One or more tests failed — check the Allure report above."
        }
    }
}

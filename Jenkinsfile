// File purpose: Defines the parameterized CI/CD workflow executed by Jenkins for every commit.
/*
 * Learning map:
 * - A Declarative Pipeline describes CI/CD as version-controlled stages.
 * - CI covers checkout, validation, tests, and packaging.
 * - CD starts only after CI succeeds and deploys an immutable image tag.
 * - Parameters let one Jenkinsfile handle staging, production, and rollback.
 * - Post conditions publish evidence and notifications even when a stage fails.
 */
pipeline {
    // A label parameter lets the same Pipeline move to a separately configured
    // Jenkins agent without changing source code (distributed-build bonus).
    agent {
        label "${params.BUILD_AGENT_LABEL}"
    }

    options {
        // These options make runs traceable, bounded, and safe from two
        // deployments changing the same environment at the same time.
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
        parallelsAlwaysFailFast()
        preserveStashes(buildCount: 5)
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
        string(
            name: 'BUILD_AGENT_LABEL',
            defaultValue: 'docker-agent',
            description: 'Docker-capable Jenkins node label used to execute the Pipeline.'
        )
        booleanParam(
            name: 'PUBLISH_ARTIFACTS',
            defaultValue: true,
            description: 'Use Nexus for Maven dependencies and publish versioned JARs/images after the quality gate. Requires nexus-publisher credentials.'
        )
        string(
            name: 'NEXUS_BASE_URL',
            defaultValue: 'http://host.docker.internal:18081',
            description: 'Nexus URL reachable from Maven build containers.'
        )
        string(
            name: 'NEXUS_DOCKER_REGISTRY',
            defaultValue: 'host.docker.internal:18082',
            description: 'Registry host:port reachable from the Jenkins Docker daemon.'
        )
        booleanParam(
            name: 'BUILD_CONTAINER_IMAGES',
            defaultValue: true,
            description: 'Build immutable Docker images after all tests pass.'
        )
        choice(
            name: 'PIPELINE_ACTION',
            choices: ['build-test-deploy', 'build-test', 'rollback'],
            description: 'Run the complete pipeline, CI only, or restore the previous healthy release.'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['staging', 'production'],
            description: 'Target environment used by deployment and rollback.'
        )
        booleanParam(
            name: 'AUTO_ROLLBACK',
            defaultValue: true,
            description: 'Restore the last known-good release when deployment verification fails.'
        )
        booleanParam(
            name: 'REQUIRE_PRODUCTION_APPROVAL',
            defaultValue: true,
            description: 'Pause production deploy/rollback until a user confirms the action.'
        )
        string(
            name: 'DEPLOY_TIMEOUT_SECONDS',
            defaultValue: '360',
            description: 'Maximum time allowed for Compose and HTTP health verification.'
        )
        string(
            name: 'NOTIFICATION_EMAIL',
            defaultValue: 'builds@example.com',
            description: 'Recipient override; the training placeholder uses the configured default recipient.'
        )
    }

    triggers {
        // Polling works with Reboot/Gitea even when Jenkins is not publicly
        // reachable. A webhook can call the same job for immediate builds.
        pollSCM('H/2 * * * *')
    }

    environment {
        // Dependency caches live in Docker volumes, not the disposable
        // workspace. This speeds later builds without hiding source changes.
        MAVEN_OPTS = '-Dmaven.repo.local=/cache/repository'
        NPM_CONFIG_CACHE = '/cache'
        IMAGE_NAMESPACE = 'nexora-commerce'
    }

    stages {
        stage('Checkout') {
            steps {
                // deleteDir prevents untracked files from an older build from
                // influencing the exact Git commit being verified now.
                deleteDir()
                checkout scm
                script {
                    env.SOURCE_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()
                    env.SHORT_COMMIT = env.SOURCE_COMMIT.take(12)
                    env.IMAGE_TAG = "${env.SHORT_COMMIT}-${env.BUILD_NUMBER}"
                    env.ARTIFACT_VERSION = "1.0.${env.BUILD_NUMBER}-${env.SHORT_COMMIT}"
                    // Nested build containers do not inherit Docker Desktop's
                    // special host DNS entry. Resolve it on the outer agent.
                    env.CI_HOST_IP = sh(
                        script: "getent ahostsv4 host.docker.internal | awk 'NR == 1 {print \$1}'",
                        returnStdout: true
                    ).trim() ?: 'host-gateway'
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.SHORT_COMMIT}"
                    currentBuild.description = "${params.PIPELINE_ACTION} → ${params.DEPLOY_ENV}"
                }
            }
        }

        stage('Validate Configuration') {
            steps {
                sh '''
                    bash scripts/ci/validate.sh

                    # Docker creates named volumes as root. Initialize each
                    # dependency cache for UID 1000 before the non-root Maven
                    # and Node stage containers use it.
                    for cache in nexora-commerce-maven-cache nexora-commerce-npm-cache; do
                      docker volume create "$cache" >/dev/null
                      docker run --rm \
                        -v "$cache:/cache" \
                        alpine:3.22 \
                        chown -R 1000:1000 /cache
                    done
                '''
            }
        }

        stage('Build and Test') {
            when {
                expression {
                    params.PIPELINE_ACTION != 'rollback'
                }
            }
            failFast true
            parallel {
                // Backend and frontend are independent CI branches. Running
                // them in parallel reduces feedback time; failFast stops the
                // sibling branch when one side proves the commit is invalid.
                stage('Backend — Java 17 / Maven') {
                    agent {
                        docker {
                            image 'maven:3.9.11-eclipse-temurin-17'
                            args "--add-host host.docker.internal:${env.CI_HOST_IP} -v nexora-commerce-maven-cache:/cache -v /nexora-commerce-certs:/usr/local/share/ca-certificates/nexora-commerce:ro"
                            reuseNode true
                        }
                    }
                    steps {
                        sh '''
                            # Stage containers run as non-root UID 1000. Build a
                            # writable Java truststore instead of modifying the
                            # image-wide operating-system CA directory.
                            first_ca="$(find /usr/local/share/ca-certificates/nexora-commerce -type f -name '*.crt' -print -quit 2>/dev/null)"
                            if [ -n "${first_ca}" ]; then
                              ci_truststore="${WORKSPACE}@tmp/nexora-commerce-cacerts"
                              cp "${JAVA_HOME}/lib/security/cacerts" "${ci_truststore}"
                              keytool -importcert -noprompt \
                                -alias nexora-commerce-host-ca \
                                -file "${first_ca}" \
                                -keystore "${ci_truststore}" \
                                -storepass changeit >/dev/null
                              printf '%s\n' "-Djavax.net.ssl.trustStore=${ci_truststore}" "-Djavax.net.ssl.trustStorePassword=changeit" > .mvn/jvm.config
                            fi
                            # Nexus-enabled builds run below with scoped credentials.
                        '''
                        script {
                            if (params.PUBLISH_ARTIFACTS) {
                                withCredentials([usernamePassword(credentialsId: 'nexus-publisher',
                                    usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                                    sh 'bash scripts/ci/nexus-maven.sh verify'
                                }
                            } else {
                                sh 'mvn -B -ntp -Drevision="$ARTIFACT_VERSION" clean verify'
                            }
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults: '**/target/surefire-reports/*.xml',
                                allowEmptyResults: true,
                                keepLongStdio: true
                            )
                        }
                        success {
                            archiveArtifacts(
                                artifacts: '**/target/*.jar',
                                fingerprint: true
                            )
                        }
                    }
                }

                stage('Frontend — Angular / Node 24') {
                    agent {
                        docker {
                            image 'node:24-bookworm-slim'
                            args "--add-host host.docker.internal:${env.CI_HOST_IP} -v nexora-commerce-npm-cache:/cache -v /nexora-commerce-certs:/usr/local/share/ca-certificates/nexora-commerce:ro"
                            reuseNode true
                        }
                    }
                    steps {
                        sh '''
                            # Node accepts an extra PEM root without modifying
                            # its read-only system trust store or requiring root.
                            first_ca="$(find /usr/local/share/ca-certificates/nexora-commerce -type f -name '*.crt' -print -quit 2>/dev/null)"
                            if [ -n "${first_ca}" ]; then
                              export NODE_EXTRA_CA_CERTS="${first_ca}"
                            fi
                            cd frontend
                            npm ci
                            # Audit production dependencies before tests; high
                            # severity advisories stop the same stage.
                            npm audit --omit=dev --audit-level=high
                            npm run test:ci
                            node ../scripts/motion-test.mjs
                            npm run build
                        '''
                    }
                    post {
                        always {
                            junit(
                                testResults: 'frontend/test-results/junit.xml',
                                allowEmptyResults: true,
                                keepLongStdio: true
                            )
                        }
                        success {
                            archiveArtifacts(
                                artifacts: 'frontend/dist/**,frontend/coverage/**',
                                fingerprint: true
                            )
                        }
                    }
                }
            }
        }

        stage('Static Analysis and Quality Gate') {
            when {
                expression {
                    params.PIPELINE_ACTION != 'rollback'
                }
            }
            steps {
                withCredentials([
                    string(
                        credentialsId: 'sonarqube-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    script {
                        int analysisExit = sh(
                            script: 'bash scripts/ci/sonarqube.sh',
                            returnStatus: true
                        )
                        env.QUALITY_GATE_RESULT = analysisExit == 0 ? 'PASSED' : 'FAILED'
                        if (analysisExit != 0) {
                            error('SonarQube Quality Gate failed; image build and deployment are blocked.')
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts(
                        artifacts: '.scannerwork/report-task.txt,quality/reports/report-task.txt',
                        allowEmptyArchive: true,
                        fingerprint: true
                    )
                }
            }
        }

        stage('Publish Maven Artifacts') {
            when {
                expression { params.PUBLISH_ARTIFACTS && params.PIPELINE_ACTION != 'rollback' }
            }
            agent {
                docker {
                    image 'maven:3.9.11-eclipse-temurin-17'
                    args "--add-host host.docker.internal:${env.CI_HOST_IP} -v nexora-commerce-maven-cache:/cache"
                    reuseNode true
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-publisher',
                    usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    // Same version and workspace as verify; only after Sonar passes.
                    sh 'bash scripts/ci/nexus-maven.sh deploy'
                }
            }
        }

        stage('Build Immutable Images') {
            when {
                expression {
                    params.PIPELINE_ACTION != 'rollback' &&
                        (params.BUILD_CONTAINER_IMAGES || params.PUBLISH_ARTIFACTS ||
                         params.PIPELINE_ACTION == 'build-test-deploy')
                }
            }
            steps {
                sh 'bash scripts/ci/build-artifact-images.sh'
            }
            post {
                success {
                    archiveArtifacts(artifacts: 'test-results/images/manifest.txt', fingerprint: true)
                }
            }
        }

        stage('Publish Docker Artifacts') {
            when {
                expression { params.PUBLISH_ARTIFACTS && params.PIPELINE_ACTION != 'rollback' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-publisher',
                    usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    sh 'bash scripts/ci/nexus-images.sh push'
                }
                archiveArtifacts(artifacts: 'test-results/images/nexus-push.txt', fingerprint: true)
            }
        }

        stage('Production Approval') {
            when {
                allOf {
                    expression {
                        params.DEPLOY_ENV == 'production'
                    }
                    expression {
                        params.PIPELINE_ACTION in ['build-test-deploy', 'rollback']
                    }
                    expression {
                        params.REQUIRE_PRODUCTION_APPROVAL
                    }
                }
            }
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input(
                        message: "Approve ${params.PIPELINE_ACTION} for production?",
                        ok: 'Approve',
                        submitterParameter: 'APPROVED_BY'
                    )
                }
            }
        }

        stage('Deploy and Verify') {
            when {
                expression {
                    params.PIPELINE_ACTION == 'build-test-deploy'
                }
            }
            steps {
                withCredentials([
                    string(
                        credentialsId: 'marketplace-jwt-secret',
                        variable: 'JWT_SECRET'
                    ),
                    string(
                        credentialsId: 'marketplace-internal-token',
                        variable: 'INTERNAL_SERVICE_TOKEN'
                    ),
                    usernamePassword(
                        credentialsId: 'marketplace-mongo-credentials',
                        usernameVariable: 'MONGO_ROOT_USERNAME',
                        passwordVariable: 'MONGO_ROOT_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'marketplace-minio-credentials',
                        usernameVariable: 'MINIO_ROOT_USER',
                        passwordVariable: 'MINIO_ROOT_PASSWORD'
                    )
                ]) {
                    script {
                        env.DEPLOYMENT_RESULT = 'IN_PROGRESS'
                        int deployExit = sh(
                            script: "bash scripts/ci/deploy.sh '${params.DEPLOY_ENV}' '${env.IMAGE_TAG}'",
                            returnStatus: true
                        )

                        if (deployExit == 0) {
                            env.DEPLOYMENT_RESULT = 'SUCCESS'
                        } else {
                            env.DEPLOYMENT_RESULT = 'FAILED'

                            if (params.AUTO_ROLLBACK) {
                                env.ROLLBACK_RESULT = 'IN_PROGRESS'
                                int rollbackExit = sh(
                                    script: "bash scripts/ci/rollback.sh '${params.DEPLOY_ENV}' current",
                                    returnStatus: true
                                )
                                env.ROLLBACK_RESULT = rollbackExit == 0 ? 'SUCCESS' : 'FAILED'
                            } else {
                                env.ROLLBACK_RESULT = 'DISABLED'
                            }

                            error(
                                "Deployment failed; rollback result is ${env.ROLLBACK_RESULT}."
                            )
                        }
                    }
                }
            }
        }

        stage('Manual Rollback') {
            when {
                expression {
                    params.PIPELINE_ACTION == 'rollback'
                }
            }
            steps {
                script {
                    env.ROLLBACK_RESULT = 'IN_PROGRESS'
                    int rollbackExit = sh(
                        script: "bash scripts/ci/rollback.sh '${params.DEPLOY_ENV}' previous",
                        returnStatus: true
                    )
                    env.ROLLBACK_RESULT = rollbackExit == 0 ? 'SUCCESS' : 'FAILED'

                    if (rollbackExit != 0) {
                        error("Manual rollback failed for ${params.DEPLOY_ENV}.")
                    }
                }
            }
        }
    }

    post {
        success {
            echo "CI succeeded for ${env.SOURCE_COMMIT}; image tag is ${env.IMAGE_TAG}."
            script {
                sendBuildNotifications('SUCCESS')
            }
        }
        failure {
            echo 'CI failed. Declarative Pipeline stops before deployment when any stage fails.'
            script {
                sendBuildNotifications('FAILURE')
            }
        }
        unstable {
            script {
                sendBuildNotifications('UNSTABLE')
            }
        }
        aborted {
            script {
                sendBuildNotifications('ABORTED')
            }
        }
        always {
            archiveArtifacts(
                artifacts: 'test-results/deployment/*.log',
                allowEmptyArchive: true
            )
            // Restore workspace ownership even if another Docker tool creates
            // root-owned output during a failed or aborted build.
            sh(
                script: '''
                    docker run --rm \
                      -v "$WORKSPACE:/workspace" \
                      alpine:3.22 \
                      chown -R 1000:1000 /workspace
                ''',
                returnStatus: true
            )
            echo "Result: ${currentBuild.currentResult}"
        }
    }
}

// Post-condition helpers live outside the Declarative block so the main stage
// flow stays readable. Notification failures are logged without replacing the
// real build/deployment result that the team needs to investigate.
def sendBuildNotifications(String buildStatus) {
    String requestedRecipient = params.NOTIFICATION_EMAIL?.trim()
    // Automatic SCM runs use the Jenkins job's clone-safe placeholder. Replace
    // only that placeholder with the runtime default so local Mailpit still
    // works while a configured Gmail installation reaches the real recipient.
    String recipient = requestedRecipient
    if (!recipient || recipient == 'builds@example.com') {
        recipient = env.DEFAULT_NOTIFICATION_EMAIL?.trim()
    }
    String subject = "[Nexora Commerce] ${buildStatus}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    String body = """Nexora Commerce pipeline result: ${buildStatus}

Job: ${env.JOB_NAME} #${env.BUILD_NUMBER}
Action: ${params.PIPELINE_ACTION}
Environment: ${params.DEPLOY_ENV}
Commit: ${env.SOURCE_COMMIT ?: 'unknown'}
Deployment: ${env.DEPLOYMENT_RESULT ?: 'not-run'}
Rollback: ${env.ROLLBACK_RESULT ?: 'not-run'}
Quality Gate: ${env.QUALITY_GATE_RESULT ?: 'not-run'}
Details: ${env.BUILD_URL}
"""

    if (recipient) {
        try {
            if (!fileExists('scripts/ci/send-email.sh')) {
                error('Email notification script is unavailable in the checked-out revision.')
            }

            int emailExit = 1
            withEnv([
                "EMAIL_TO=${recipient}",
                "EMAIL_SUBJECT=${subject}",
                "EMAIL_BODY=${body}"
            ]) {
                String smtpCredentialsId = env.SMTP_CREDENTIALS_ID?.trim()
                if (smtpCredentialsId) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: smtpCredentialsId,
                            usernameVariable: 'SMTP_USERNAME',
                            passwordVariable: 'SMTP_APP_PASSWORD'
                        )
                    ]) {
                        emailExit = sh(
                            script: 'bash scripts/ci/send-email.sh',
                            returnStatus: true
                        )
                    }
                } else {
                    emailExit = sh(
                        script: 'bash scripts/ci/send-email.sh',
                        returnStatus: true
                    )
                }
            }

            if (emailExit != 0) {
                error("SMTP notification failed with exit code ${emailExit}.")
            }
            echo "Email notification delivered to the configured SMTP server for ${recipient}."
        } catch (Exception emailError) {
            echo "Email notification failed: ${emailError.message}"
        }
    } else {
        echo 'Email notification skipped because NOTIFICATION_EMAIL is empty.'
    }

    if (!fileExists('scripts/ci/notify.sh')) {
        echo 'Slack notification skipped because checkout did not provide notify.sh.'
        return
    }

    try {
        withCredentials([
            string(
                credentialsId: 'slack-webhook',
                variable: 'SLACK_WEBHOOK_URL'
            )
        ]) {
            int slackExit = sh(
                script: "bash scripts/ci/notify.sh '${buildStatus}'",
                returnStatus: true
            )
            if (slackExit != 0) {
                echo "Slack notification failed with exit code ${slackExit}."
            }
        }
    } catch (Exception slackError) {
        echo "Slack notification could not run: ${slackError.message}"
    }
}

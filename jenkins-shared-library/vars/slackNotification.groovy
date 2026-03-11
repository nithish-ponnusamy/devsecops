// vars/slackNotification.groovy
// Jenkins Shared Library – Slack notification function

def call(String buildStatus = 'STARTED') {
    def color
    def emoji
    def statusMessage

    switch (buildStatus) {
        case 'STARTED':
            color   = '#439FE0'   // blue
            emoji   = ':rocket:'
            statusMessage = 'Pipeline Started'
            break
        case 'SUCCESS':
            color   = '#36A64F'   // green
            emoji   = ':white_check_mark:'
            statusMessage = 'Pipeline Succeeded'
            break
        case 'FAILURE':
            color   = '#FF0000'   // red
            emoji   = ':x:'
            statusMessage = 'Pipeline Failed'
            break
        case 'UNSTABLE':
            color   = '#FFA500'   // orange
            emoji   = ':warning:'
            statusMessage = 'Pipeline Unstable'
            break
        default:
            color   = '#808080'   // grey
            emoji   = ':question:'
            statusMessage = 'Pipeline Status Unknown'
    }

    def summary = """
        ${emoji} *${statusMessage}*
        *Job:* ${env.JOB_NAME}
        *Build:* #${env.BUILD_NUMBER}
        *Branch:* ${env.GIT_BRANCH ?: 'N/A'}
        *Duration:* ${currentBuild.durationString?.replace(' and counting', '') ?: 'N/A'}
        *Triggered by:* ${currentBuild.getBuildCauses()[0]?.shortDescription ?: 'Unknown'}
        *Console:* <${env.BUILD_URL}|View Logs>
    """.stripIndent().trim()

    slackSend(
        channel: '#devsecops-pipeline',
        color: color,
        message: summary,
        teamDomain: 'your-slack-workspace',
        tokenCredentialId: 'slack-bot-token'
    )
}

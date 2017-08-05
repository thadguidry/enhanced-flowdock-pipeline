// Based on:
// https://github.com/jenkinsci/flowdock-plugin/issues/24#issuecomment-271784565
import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result


def call(script, apiToken, tags = '', sendForSuccess = 'NO_CHAT_FOR_SUCCESS') {  //Alternative default: SEND_CHAT_FOR_SUCCESS

    tags = tags.replaceAll("\\s","")

    def flowdockURL = "https://api.flowdock.com/v1/messages/team_inbox/${apiToken}"
    def flowdockChatURL = "https://api.flowdock.com/v1/messages/chat/${apiToken}"

    // build status of null means successful
    def buildStatus =  script.currentBuild.result ? script.currentBuild.result : 'SUCCESS'

    // create subject
    def subject = "${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName.replaceAll("#", "")}"

    // we use the build+XX@flowdock.com addresses for their yay/nay avatars
    def fromAddress = ''

    // update subject and set from address based on build status
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          fromAddress = 'build+ok@flowdock.com'
          break
        }
        subject += ' was successful'
        fromAddress = 'build+ok@flowdock.com'
        break
      case 'FAILURE':
        subject += ' failed'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'ABORTED':
        subject += ' was aborted'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'FIXED':
        subject = ' was fixed'
        fromAddress = 'build+ok@flowdock.com'
        break
    }

    // build message
    def content = """<h3>${script.env.JOB_BASE_NAME}</h3>
      Build: ${script.currentBuild.displayName}<br />
      Result: <strong>${buildStatus}</strong><br />
      URL: <a href="${script.env.BUILD_URL}">${script.currentBuild.fullDisplayName}</a><br />"""

    // add Gerrit trigger and branch information if available
    def GERRIT_REFNAME =  script.env.GERRIT_REFNAME ? script.env.GERRIT_REFNAME : 'none'
    if (GERRIT_REFNAME != "none") {
        content += """<br /><strong>Version control:</strong><br />
            Gerrit trigger: ${script.env.GERRIT_EVENT_TYPE}<br/>
            Gerrit branch: ${script.env.GERRIT_REFNAME}<br/>
            Gerrit URL: <a href="${script.env.GERRIT_SCHEME}://${script.env.CI_USER}@${script.env.GERRIT_HOST}:${script.env.GERRIT_PORT}/${script.env.GERRIT_PROJECT}">${script.env.GERRIT_SCHEME}://${script.env.CI_USER}@${script.env.GERRIT_HOST}:${script.env.GERRIT_PORT}/${script.env.GERRIT_PROJECT}<br/>
            <br/>"""
      }


    // build payload
    def payload = JsonOutput.toJson([source : "Jenkins",
                                     project : script.env.JOB_BASE_NAME,
                                     from_address: fromAddress,
                                     from_name: 'Jenkins',
                                     subject: subject,
                                     tags: tags,
                                     content: content,
                                     link: script.env.BUILD_URL
                                     ])

    def response = httpRequest acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: payload, url: flowdockURL
    
    
    //send chat input based on build status. 
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          content = """:white_check_mark: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was fixed**](${script.env.BUILD_URL})"""
          break
        }
        content = """:white_check_mark: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was successful**](${script.env.BUILD_URL})"""

        break
      case 'FAILURE':
        content = """:x: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **failed**](${script.env.BUILD_URL})"""
        break
      case 'UNSTABLE':
        content = """:heavy_exclamation_mark: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was unstable**](${script.env.BUILD_URL})"""
        break
      case 'ABORTED':
        content = """:no_entry_sign: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was aborted**](${script.env.BUILD_URL})"""
        break
      case 'NOT_BUILT':
        content = """:o: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was not built**](${script.env.BUILD_URL})"""
        break
      case 'FIXED':
        content = """:white_check_mark: [${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName} **was fixed**](${script.env.BUILD_URL})"""
        break
    }
    content = content.replaceAll("#","")
    payload = JsonOutput.toJson([external_user_name: 'Jenkins',
                                     tags: tags,
                                     content: content
                                     ])
                                     
    //Don't send chat notification if not wanted and result is success and not recovered failure
    if (buildStatus.equals('SUCCESS') && sendForSuccess.equals('NO_CHAT_FOR_SUCCESS')) {
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
            response = httpRequest acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: payload, url: flowdockChatURL
        }
    } else {
        response = httpRequest acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: payload, url: flowdockChatURL
    }    

}

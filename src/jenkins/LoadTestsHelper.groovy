package jenkins

// Helper class for all load tests operations.
public class LoadTestsHelper {

    private Map config = null;
    def steps

    LoadTestsHelper(steps) {
        this.steps = steps
    }

    private void getConfig(String env) {
        if (null == this.config) {
            return
        }
        if (null != env && null != this.config.environments && null != this.config.environments[env]) {
        }
    }

    public Map runLoadTests(Map config, Map userInput, Map loadConfig) {
        def kubeconfig = loadConfig.kubeconfig
        def infraYaml = loadConfig.infraYaml
        def pods = userInput.pods
        def peakTps = userInput.peaktps
        def rampuptime = userInput.rampuptime
        def steadystatetime = userInput.steadystatetime
        def query = userInput.query
        def simulationClass = userInput.simulationClass
        def Custom = userInput.Custom
        def url = userInput.url
        def generatedName = userInput.generatedName
        def gitUrl = loadConfig.gitUrl
        def buildUrl = steps.env.BUILD_URL
        def envName = loadConfig.envName
        def buildNum = steps.env.BUILD_NUMBER
        def uniqueName = loadConfig.uniqueName
        def accountName = loadConfig.accountName
        def pfiNamespace = loadConfig.pfiNamespace
        def currentloadTest = [:]
        if (generatedName == null) {
            generatedName = "pfi-"
        }

        try {
            steps.sh("argo version")
            if (kubeconfig?.trim()) {
                steps.sh("argo list --kubeconfig ${kubeconfig}")
                steps.sh("argo submit ${infraYaml} -psimulationClass=${simulationClass} --generate-name ${generatedName} -pquery=${query} -plimit=${pods} -ppeakTPS=${peakTps} -prampupTime=${rampuptime} -psteadyStateTime=${steadystatetime} -pCustom=${Custom} -pbaseurl=${url} -pgitrepo=\$(echo ${gitUrl} | cut -d'/' -f5) -pbuildURL=${buildUrl} -penvName=${envName} -pbuildnum=${buildNum} -puniqueName=${uniqueName} -ppfiNamespace=${pfiNamespace} --serviceaccount ${accountName} --kubeconfig ${kubeconfig} --watch")
            } else {
                steps.sh("argo submit ${infraYaml} -psimulationClass=${simulationClass} --generate-name ${generatedName} -pquery=${query} -plimit=${pods} -ppeakTPS=${peakTps} -prampupTime=${rampuptime} -psteadyStateTime=${steadystatetime} -pCustom=${Custom} -pbaseurl=${url} -pgitrepo=\$(echo ${gitUrl} | cut -d'/' -f5) -pbuildURL=${buildUrl} -penvName=${envName} -pbuildnum=${buildNum} -puniqueName=${uniqueName} --serviceaccount ${accountName} --watch")
            }
        } catch (Exception e) {
            steps.echo "Caught Exception, msg = ${e.getMessage()}"
        }

        return currentloadTest
    }

    public void downloadResultsFromS3(Map config, Map s3Config) {
        steps.sh script:"""
                echo "${s3Config.s3Bucket}" > /tmp/s3Bucket
                echo "${s3Config.accessRole}" > /tmp/accessRole
                echo "${s3Config.uniqueNum}" > /tmp/uniqueNum
                echo "${steps.env.WORKSPACE}" > /tmp/workspacePath
                echo "${s3Config.pfiNamespace}" > /tmp/pfiNamespace
                echo "${s3Config.s3WaitTimeSec}" > /tmp/s3WaitTimeSec
                echo "${s3Config.dateTimeTag}" > /tmp/dateTimeTag
                echo "${s3Config.s3Path}" > /tmp/s3Path
            """
        steps.sh script: '''
                #!/bin/sh -xe
                s3Bucket=`cat /tmp/s3Bucket`
                accessRole=`cat /tmp/accessRole`
                uniqueNum=`cat /tmp/uniqueNum`
                workspacePath=`cat /tmp/workspacePath`
                pfiNamespace=`cat /tmp/pfiNamespace`
                s3WaitTimeSec=`cat /tmp/s3WaitTimeSec`
                dateTimeTag=`cat /tmp/dateTimeTag`
                s3Path=`cat /tmp/s3Path`
                rm -f /tmp/accessRole /tmp/uniqueNum /tmp/workspacePath /tmp/pfiNamespace /tmp/s3WaitTimeSec
                tempRole=$(aws sts assume-role --role-arn ${accessRole} --role-session-name AWSCLI-Session)
                export AWS_ACCESS_KEY_ID=$(echo ${tempRole} | jq .Credentials.AccessKeyId | xargs)
                export AWS_SECRET_ACCESS_KEY=$(echo ${tempRole} | jq .Credentials.SecretAccessKey | xargs)
                export AWS_SESSION_TOKEN=$(echo ${tempRole} | jq .Credentials.SessionToken | xargs)
                sleep ${s3WaitTimeSec}
                aws sts get-caller-identity
                mkdir -p ${workspacePath}/results/gatlingsimulation-${dateTimeTag}
                aws s3 ls s3://${s3Bucket}/${s3Path}
                aws s3 cp s3://${s3Bucket}/${s3Path} ${workspacePath}/results/gatlingsimulation-${dateTimeTag} --recursive
                ls -R ${workspacePath}/results/
            '''
    }

}

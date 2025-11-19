@Library("ci@master") _

MavenDockerPublish(
    'tenant': 'omega',
    'pipelineTimeout': '15',
    'mavenDeployCommand': 'mvn clean package -DskipTests',
    'enableCodeSonarSastScan': true,
    'enableCodeSonarSastGating': false,
    'codeSonarScanCommand': 'mvn clean verify sonar:sonar',
    'disableImagePublish': false,
    'jdkVersion': '17'
)
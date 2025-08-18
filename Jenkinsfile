/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
        useContainerAgent: false,
  forkCount: '1C', // Set to `false` if you need to use Docker for containerized tests
  configurations: [
    [platform: 'arm64linux', jdk: 21],
    [platform: 'windows', jdk: 17],
    [platform: 'linux', jdk: 25],
])

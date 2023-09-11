#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(useContainerAgent: true, configurations: [
  [platform: 'linux',   jdk: 21], // Linux first for coverage report on ci.jenkins.io
  [platform: 'linux', jdk: 17],
  [platform: 'windows', jdk: 11],
])

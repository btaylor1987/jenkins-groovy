def wineBuild = '''
#!/bin/bash
mkdir build64
cd build64
../configure --enable-win64
make -j4
'''
def wineGitUrl='https://github.com/wine-mirror/wine.git'
def wineGitBranch='*/stable'

// Using a number based loop instead of each since that's how I set everything up anyway
// In real world use we should have a struct of POPs or similar, and then we can iterate
// over those while creating jobs _and_ pipelines this way we only need to add/remove a pop
// in that struct to allow us to add it to jobs in Jenkins master.  EZPZ (Till it isn't ;) ).
for(i in 1..2) {
  job("C${i} Wine64-DSL") {
    label("test${i}-jenkins-agent")
    scm {
      git {
        remote {
          url(wineGitUrl)
        }
        branch(wineGitBranch)
      }
    }
    steps {
      shell(wineBuild)
    }
  }
  job("C${i} Hello World-DSL") {
    label("test${i}-jenkins-agent")
    steps {
      shell('echo "Hello World"')
      shell('sleep 60')
    }
  }
}

pipelineJob('serial-DSL'){
  definition {
    cps {
//TODO: Generate this based on a for loop
      script('''
pipeline {
    agent none
    stages {
        stage('Cluster 1 HW') {
            steps {
                build 'C1 Hello World-DSL'
            }
        }
        stage('Cluster 2 HW') {
            steps {
                build 'C2 Hello World-DSL'
            }
        }
        stage('Cluster 1 Wine64') {
            steps {
                build 'C1 Wine64-DSL'
            }
        }
        stage('Cluster 2 Wine64') {
            steps {
                build 'C2 Wine64-DSL'
            }
        }
    }
}
   ''')
    }
  }
}

pipelineJob('parallel-DSL'){
  definition {
    cps {
      script('''
pipeline {
    agent none
    stages {
        stage("hello World") {
            parallel {
                stage("Hello C1") {
                    steps {
                        build 'C1 Hello World-DSL'
                    }
                }
                stage("Hello C2") {
                    steps {
                        build 'C2 Hello World-DSL'
                    }
                }
            }
        }
        stage("Wine 64-bit") {
            parallel {
                stage("Wine64 C1") {
                    steps {
                        build 'C1 Wine64-DSL'
                    }
                }
                stage("Wine64 C2") {
                    steps {
                        build 'C2 Wine64-DSL'
                    }
                }
            }
        }
    }
}
      ''')
    }
  }
}

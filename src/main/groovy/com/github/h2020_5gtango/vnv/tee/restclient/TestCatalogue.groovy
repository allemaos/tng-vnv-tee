/*
 * Copyright (c) 2015 SONATA-NFV, 2017 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * ALL RIGHTS RESERVED.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Neither the name of the SONATA-NFV, 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * This work has been performed in the framework of the SONATA project,
 * funded by the European Commission under Grant number 671517 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the SONATA
 * partner consortium (www.sonata-nfv.eu).
 *
 * This work has been performed in the framework of the 5GTANGO project,
 * funded by the European Commission under Grant number 761493 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the 5GTANGO
 * partner consortium (www.5gtango.eu).
 */

package com.github.h2020_5gtango.vnv.tee.restclient

import com.github.h2020_5gtango.vnv.tee.model.PackageMetadata
import com.github.h2020_5gtango.vnv.tee.model.Test
import com.github.h2020_5gtango.vnv.tee.model.TestSuite
import groovy.util.logging.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

import static com.github.h2020_5gtango.vnv.tee.helper.DebugHelper.callExternalEndpoint
import static com.github.h2020_5gtango.vnv.tee.helper.DebugHelper.loginfo

@Log
@Component
class TestCatalogue {

    @Autowired
    @Qualifier('restTemplateWithAuth')
    RestTemplate restTemplateWithAuth

    @Autowired
    @Qualifier('restTemplateWithAuth')
    RestTemplate restTemplate


    @Value('${app.cat.package.metadata.endpoint}')
    def packageLoadEndpoint

    @Value('${app.cat.test.list.endpoint}')
    def testListEndpoint

    @Value('${app.cat.test.metadata.endpoint}')
    def testSuiteLoadEndpoint

    @Value('${app.cat.resource.download.endpoint}')
    def resourceDownloadEndpoint

    @Value('${app.tee.tmp.dir}')
    File tmpDir

    public TestCatalogue(){
        println "##vnvlog: vnv-init: new code loaded"
    }

    List<Test> listTests() {
        callExternalEndpoint(restTemplateWithAuth.getForEntity(testListEndpoint, Test[].class),'TestCatalogue.listTests',testListEndpoint).body
    }

    PackageMetadata loadPackageMetadata(String packageId ) {
        callExternalEndpoint(restTemplate.getForEntity(packageLoadEndpoint,PackageMetadata,packageId),'TestCatalogue.loadPackageMetadata',packageLoadEndpoint).body
    }

    TestSuite loadTestSuite(String testUuid ) {
        TestSuite testSuite=callExternalEndpoint(restTemplateWithAuth.getForEntity(testSuiteLoadEndpoint,TestSuite,testUuid),'TestCatalogue.loadTestSuite',testSuiteLoadEndpoint).body
        testSuite.type=testSuite.testd.test_type
        testSuite.testd.test_configuration_parameters.each{param->
            if(param.containsKey('content_type')){
                testSuite.testResources.add(new TestSuite.TestResource(
                        contentType: param['content_type'],
                        source: 'Tests/'+param['parameter_value'],
                        target: param['parameter_value'],
                ))
            }
        }
        testSuite
    }

    File downloadTestSuiteResources(PackageMetadata packageMetadata,TestSuite testSuite,String testSuiteResultId) {
        def testSuiteWorkingDir=new File(tmpDir,testSuiteResultId)
        testSuite.testResources.each {testResource->
            def targetFile=new File(testSuiteWorkingDir,testResource.target?:testResource.source)
            targetFile.parentFile.mkdirs()
            targetFile.delete()

            def resourceUuid=packageMetadata.pd.package_content.find{it.source==testResource.source}.uuid
            packageMetadata.pd.package_content.each{pc -> loginfo("##vnvlog-v.3: --packageMetadataUuid: ${packageMetadata.uuid} [testSuiteResultId: ${testSuiteResultId}] --testSuiteUuid: ${resourceUuid} --testResourceSource: ${testResource.source}")}
            targetFile << callExternalEndpoint(
                    restTemplate.getForEntity(resourceDownloadEndpoint,byte[].class,packageMetadata.uuid,resourceUuid),'TestCatalogue.downloadTestSuiteResources',resourceDownloadEndpoint).body
        }
        testSuiteWorkingDir
    }

}

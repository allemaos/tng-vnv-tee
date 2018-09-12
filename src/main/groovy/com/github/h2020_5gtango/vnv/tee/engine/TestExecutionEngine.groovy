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

package com.github.h2020_5gtango.vnv.tee.engine

import com.github.h2020_5gtango.vnv.tee.model.TestSuite
import com.github.h2020_5gtango.vnv.tee.model.TestSuiteResult
import com.github.h2020_5gtango.vnv.tee.restclient.TestCatalogue
import com.github.h2020_5gtango.vnv.tee.restclient.TestResultRepository
import com.github.h2020_5gtango.vnv.tee.tester.Tester
import groovy.text.SimpleTemplateEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestExecutionEngine {

    @Autowired
    TestResultRepository testResultRepository

    @Autowired
    TestCatalogue testCatalogue

    @Autowired
    Map<String, Tester> testers

    @PostMapping('/api/v1/test-suite-results')
    TestSuiteResult executeTestAgainstNs(@RequestBody TestSuiteResult testSuiteResult) {
        testSuiteResult=testResultRepository.processTestSuiteResult(testSuiteResult, 'LOADING')
        def packageMetadata = testCatalogue.loadPackageMetadata(testSuiteResult.packageId)
        def testSuite = testCatalogue.loadTestSuite(testSuiteResult.testUuid)
        def testWorkspace = testCatalogue.downloadTestSuiteResources(packageMetadata,testSuite, testSuiteResult.uuid)
        def nsi = testResultRepository.loadNetworkServiceInstance(testSuiteResult.instanceUuid)
        applyTemplating(testSuite, testWorkspace, nsi)
        testSuiteResult=testResultRepository.processTestSuiteResult(testSuiteResult, 'TESTING')
        testSuiteResult = testers[testSuite.type].execute(testWorkspace, testSuiteResult)
        testResultRepository.processTestSuiteResult(testSuiteResult)
    }

    List<TestSuite.TestResource> applyTemplating(testSuite, testWorkspace, nsi) {
        testSuite.testResources.each { testResource ->
            if (testResource.contentType.contains('text')) {
                def targetFile = new File(testWorkspace, testResource.target ?: testResource.source)
                def binding = [
                        nsi      : nsi,
                        testSuite: testSuite,
                        workspace: testWorkspace,
                ]
                def replacedText = new SimpleTemplateEngine().createTemplate(targetFile.text).make(binding).toString()
                targetFile.delete()
                targetFile << replacedText
            }
        }
    }

}

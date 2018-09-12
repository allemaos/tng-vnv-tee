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

import com.github.h2020_5gtango.vnv.tee.model.NetworkServiceInstance
import com.github.h2020_5gtango.vnv.tee.model.TestSuiteResult
import groovy.util.logging.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

import static com.github.h2020_5gtango.vnv.tee.helper.DebugHelper.callExternalEndpoint

@Log
@Component
class TestResultRepository {

    @Autowired
    @Qualifier('restTemplateWithAuth')
    RestTemplate restTemplate

    @Value('${app.trr.test.suite.result.create.endpoint}')
    def testSuiteResultCreateEndpoint

    @Value('${app.trr.test.suite.result.update.endpoint}')
    def testSuiteResultUpdateEndpoint

    @Value('${app.trr.network.service.instance.load.endpoint}')
    def networkServiceInstanceLoadEndpoint

    @Value('${app.trr.network.function.instance.load.endpoint}')
    def networkFunctionInstanceLoadEndpoint

    @Value('${app.trr.test.suite.result.filter.by.service.endpoint}')
    def resultFilterByServiceEndpoint

    @Value('${app.trr.test.suite.result.filter.by.test.endpoint}')
    def resultFilterByTestEndpoint

    TestSuiteResult createTestSuiteResult(TestSuiteResult testSuiteResult) {
        testSuiteResult.status='SCHEDULED'
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        def entity = new HttpEntity<TestSuiteResult>(testSuiteResult ,headers)
        callExternalEndpoint(restTemplate.postForEntity(testSuiteResultCreateEndpoint,entity,TestSuiteResult),'TestResultRepository.createTestSuiteResult',testSuiteResultCreateEndpoint).body
    }

    TestSuiteResult processTestSuiteResult(TestSuiteResult testSuiteResult) {
        testSuiteResult.status='TESTING'
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        def entity = new HttpEntity<TestSuiteResult>(testSuiteResult ,headers)
        log.info("##vnvlog-v.3: testSuiteResult.uuid is ${testSuiteResult.uuid}")
        callExternalEndpoint(restTemplate.postForEntity(testSuiteResultCreateEndpoint,entity,TestSuiteResult),'TestResultRepository.createTestSuiteResult',testSuiteResultCreateEndpoint).body
    }

    TestSuiteResult debuggingTestSuiteResult(TestSuiteResult testSuiteResult, def status) {
        testSuiteResult.status=status
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        def entity = new HttpEntity<TestSuiteResult>(testSuiteResult ,headers)
        log.info("##vnvlog-v.4: testSuiteResult.uuid is ${testSuiteResult.uuid} and status is $status")
        callExternalEndpoint(restTemplate.postForEntity(testSuiteResultUpdateEndpoint,entity,TestSuiteResult),'TestResultRepository.createTestSuiteResult',testSuiteResultCreateEndpoint).body
    }

    TestSuiteResult updateTestSuiteResult(TestSuiteResult testSuiteResult) {
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        def entity = new HttpEntity<TestSuiteResult>(testSuiteResult ,headers)
        callExternalEndpoint(restTemplate.exchange(testSuiteResultUpdateEndpoint, HttpMethod.PUT, entity, TestSuiteResult.class ,testSuiteResult.uuid),'TestResultRepository.updateTestSuiteResult',testSuiteResultUpdateEndpoint).body
    }

    NetworkServiceInstance loadNetworkServiceInstance(String instanceUuid) {
        NetworkServiceInstance networkServiceInstance = callExternalEndpoint(restTemplate.getForEntity(networkServiceInstanceLoadEndpoint, NetworkServiceInstance, instanceUuid),'TestResultRepository.loadNetworkServiceInstance',networkServiceInstanceLoadEndpoint).body
        networkServiceInstance.networkFunctions?.each{vnf->
            def vnfi = callExternalEndpoint(restTemplate.getForEntity(networkFunctionInstanceLoadEndpoint, Object.class, vnf.vnfr_id),'TestResultRepository.loadNetworkServiceInstance',networkServiceInstanceLoadEndpoint).body
            vnf.vnfi=vnfi
            vnfi.virtual_deployment_units?.each{unit->
                unit.vnfc_instance?.each{vnfc_instance->
                    def points=[:]
                    networkServiceInstance.connectionPoints.put(unit.vdu_reference,points)
                    vnfc_instance.connection_points?.each{connection_point->
                        points.put(connection_point.id,connection_point)
                    }

                }
            }
        }
        networkServiceInstance
    }

    List<TestSuiteResult> listByService(String serviceUuid) {
        callExternalEndpoint(restTemplate.getForEntity(resultFilterByServiceEndpoint, TestSuiteResult[].class,serviceUuid),'TestResultRepository.listByService',resultFilterByServiceEndpoint).body
    }

    List<TestSuiteResult> listByTest(String testUuid) {
        callExternalEndpoint(restTemplate.getForEntity(resultFilterByServiceEndpoint, TestSuiteResult[].class,testUuid),'TestResultRepository.listByTest',resultFilterByServiceEndpoint).body
    }
}

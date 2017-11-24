package ch.icclab.sentinel;

/*
 * Copyright (c) 2017. ZHAW - ICCLab
 *  All Rights Reserved.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may
 *     not use this file except in compliance with the License. You may obtain
 *     a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *     WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *     License for the specific language governing permissions and limitations
 *     under the License.
 */

/*
 *     Author: Piyush Harsh,
 *     URL: piyush-harsh.info
 */

import ch.icclab.sentinel.dao.*;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scala.App;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

//@RequestMapping("/v1")
//@RequestMapping("/${published.api.version}") //using this breaks Spring jUnit test
@RestController
@RequestMapping("/v1")
public class APIController {
    final static Logger logger = Logger.getLogger(APIController.class);

    @RequestMapping(value = {"/api/"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "getApis", notes = "List of all supported API calls")
    @ApiResponses({
            @ApiResponse(code = 200, message = "ok")
    })
    public @ResponseBody
    ResponseEntity getApis()
    {
        LinkedList<APIEndpoints> value = new LinkedList<>();
        APIEndpoints value3 = new APIEndpoints();
        value3.endpoint = "/v1/api/";
        value3.method = "GET";
        value3.description = "get list of all supported APIs";
        value3.contentType = "application/json";
        value.add(value3);
        APIEndpoints value1 = new APIEndpoints();
        value1.endpoint = "/v1/api/user/"; //each space corresponds to an individual db and vectors in a space make for tables in a db
        value1.method = "POST";
        value1.description = "add a new user ";
        value1.contentType = "application/json";
        value.add(value1);
        APIEndpoints value2 = new APIEndpoints();
        value2.endpoint = "/v1/api/user/{id}";
        value2.method = "GET";
        value2.description = "retrieve info about existing user";
        value2.contentType = "application/json";
        value.add(value2);
        APIEndpoints value4 = new APIEndpoints();
        value4.endpoint = "/v1/api/space/";
        value4.method = "POST";
        value4.description = "register a new monitored space";
        value4.contentType = "application/json";
        value.add(value4);
        APIEndpoints value5 = new APIEndpoints();
        value5.endpoint = "/v1/api/series/";
        value5.method = "POST";
        value5.description = "register a new series within a space";
        value5.contentType = "application/json";
        value.add(value5);
        APIEndpoints value6 = new APIEndpoints();
        value6.endpoint = "/v1/api/key/{id}";
        value6.method = "GET";
        value6.description = "retrieve the api-key for an user";
        value6.contentType = "application/json";
        value.add(value6);
        APIEndpoints value7 = new APIEndpoints();
        value7.endpoint = "/v1/api/endpoint";
        value7.method = "GET";
        value7.description = "retrieve the agent's connection endpoint parameters";
        value7.contentType = "application/json";
        value.add(value7);
        APIEndpoints value8 = new APIEndpoints();
        value8.endpoint = "/v1/api/pingback/{id}";
        value8.method = "GET";
        value8.description = "retrieve the healthcheck/pingback object data";
        value8.contentType = "application/json";
        value.add(value8);
        APIEndpoints value9 = new APIEndpoints();
        value9.endpoint = "/v1/api/pingback/";
        value9.method = "POST";
        value9.description = "registers a new healthcheck/pingback object";
        value9.contentType = "application/json";
        value.add(value9);

        Gson gson = new Gson();
        String jsonInString = gson.toJson(value);
        return ResponseEntity.status(HttpStatus.OK).body(jsonInString);
    }

    @RequestMapping(value = {"/dashboard/"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "showDashboard", notes = "Displays statically displayed dashboard")
    @ApiResponses({
            @ApiResponse(code = 200, message = "ok")
    })
    public @ResponseBody
    ResponseEntity showDashboard()
    {
        String src = "http://" + AppConfiguration.getDashboardEndpoint() + "/dashboard/db/" +
                AppConfiguration.getDashboardTitle() + "?refresh=30s&orgId=1&theme=light";
        String returnVal = "<html><head></head><body style=\"background-color:black;\">";
        returnVal += "<script>\n" +
                "  function resizeIframe(obj) {\n" +
                "    obj.style.height = obj.contentWindow.document.body.scrollHeight + 'px';\n" +
                "  }\n" +
                "</script>";
        returnVal += "<div style=\"width:100%; background-color:black;\"><img src=\"http://elastest.io/images/intense/elastest-logo-dark.png\" align=\"left\"></div>";
        returnVal += "<br><br><br><br>";
        returnVal += "<iframe onload=\"resizeIframe(this)\" width=\"99%\" height=\"90%\" style=\"border:none; display:block;\" src=\"" + src + "\">";
        returnVal += "</iframe></body></html>";
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(returnVal);
    }

    @RequestMapping(value = {"/error"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "returnError", notes = "In case an api is not yet implemented")
    @ApiResponses({
            @ApiResponse(code = 501, message = "this call is not supported")
    })
    public @ResponseBody
    ResponseEntity returnError()
    {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("this call is not supported yet. visit /api/ to see supported calls");
    }

    @RequestMapping(value={"/api/user/"}, method = RequestMethod.POST, produces = {"application/json"})
    @ApiOperation(value = "createUser", notes = "Create new user account")
    @ApiResponses({
            @ApiResponse(code = 401, message = "valid admin token required"),
            @ApiResponse(code = 400, message = "check data"),
            @ApiResponse(code = 409, message = "user account already exists"),
            @ApiResponse(code = 201, message = "created"),
            @ApiResponse(code = 500, message = "error in persisting user data, please contact system admin")
    })
    public @ResponseBody
    ResponseEntity createUser(@RequestBody String reqBody, @RequestHeader(value = "x-auth-token") String adminKey)
    {
        Gson gson = new Gson();
        UserDataInput incomingData = gson.fromJson(reqBody, UserDataInput.class);
        if((adminKey == null || adminKey.trim().length() == 0) || !(adminKey != null && adminKey.equalsIgnoreCase(AppConfiguration.getAdminToken())))
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("valid admin token required");
        }
        if(!incomingData.isValidData())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check data");
        if(SqlDriver.isDuplicateUser(incomingData.login))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("user account already exists");
        else
        {
            String apiKey = UUID.randomUUID().toString();
            UserDataOutput outputData = new UserDataOutput();
            outputData.apiKey = apiKey;
            outputData.login = incomingData.login;
            int id = SqlDriver.addUser(incomingData.login.trim(), incomingData.password.trim(), apiKey);
            if(id != -1)
            {
                outputData.id = id;
                outputData.accessUrl = "/api/user/" + id;
                return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(outputData));
            }
            else
            {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error in persisting user data. please contact system admin");
            }
        }
    }

    @RequestMapping(value={"/api/pingback/{pingid}"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "locatePingData", notes = "Retrieve ping history")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 200, message = "ok")
    })
    public @ResponseBody
    ResponseEntity locatePingData(@RequestHeader(value = "x-auth-apikey") String apiKey, @RequestHeader(value = "x-auth-login") String login, @PathVariable(value="pingid") String pingid)
    {
        Gson gson = new Gson();
        int pingId = -1;
        try
        {
            pingId = Integer.parseInt(pingid);
        }
        catch(NumberFormatException nex)
        {
            //supplied value is not an id but a alpha
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check url data");
        }
        int userId = SqlDriver.getUserId(login);

        if(!SqlDriver.isValidApikey(userId, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");

        //get corresponding ping object information.
        HealthCheckOutput response = SqlDriver.getPingData(pingId, userId);
        if(response != null) {
            response.id = pingId;
            response.accessUrl = "/api/pingback/" + pingId;
            response.callHistory = Application.eventsCache.getEventTraceHistory(response.pingURL, response.reportURL);
            return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(response));
        }
        else
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("no data found");
    }

    @RequestMapping(value={"/api/user/{userid}"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "locateUserData", notes = "Retrieve user account information")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 200, message = "ok")
    })
    public @ResponseBody
    ResponseEntity locateUserData(@RequestHeader(value = "x-auth-apikey") String apiKey, @PathVariable(value="userid") String login)
    {
        Gson gson = new Gson();
        int userId = -1;
        try {
            userId = Integer.parseInt(login);
        } catch(NumberFormatException nex)
        {
            //supplied value is not an id but a login
            userId = SqlDriver.getUserId(login);
        }
        if(!SqlDriver.isValidApikey(userId, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");
        //get corresponding space and subspace information.
        UserDataOutput response = new UserDataOutput();
        response.apiKey = apiKey;
        response.id = userId;
        response.accessUrl = "/api/user/" + userId;
        response.spaces = SqlDriver.getUserSpaces(userId).toArray(new SpaceOutput[SqlDriver.getUserSpaces(userId).size()]);

        return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(response));
    }

    @RequestMapping(value={"/api/key/{userid}"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "locateUserKey", notes = "Allows retrieval of api-key of a particular user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "ok"),
            @ApiResponse(code = 400, message = "no such user exists"),
            @ApiResponse(code = 401, message = "invalid password")
            })
    public @ResponseBody
    ResponseEntity locateUserKey(@RequestHeader(value = "x-auth-password") String password, @PathVariable(value="userid") String login)
    {
        Gson gson = new Gson();
        int userId = -1;
        try {
            userId = Integer.parseInt(login);
        } catch(NumberFormatException nex)
        {
            //supplied value is not an id but a login
            userId = SqlDriver.getUserId(login);
        }
        if(userId == -1)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("no such user exists");
        if(!SqlDriver.isValidPassword(userId, password))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid password");
        String apiKey = SqlDriver.getAPIKey(userId);
        UserDataOutput val = new UserDataOutput();
        val.id = userId;
        val.accessUrl = "/api/user/" + userId;
        val.apiKey = apiKey;
        return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(val));
    }

    @RequestMapping(value={"/api/space/"}, method = RequestMethod.POST, produces = {"application/json"})
    @ApiOperation(value = "createSpace", notes = "Creates a new monitoring space for an user")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 400, message = "check data"),
            @ApiResponse(code = 409, message = "this space already exists for this user"),
            @ApiResponse(code = 201, message = "created"),
            @ApiResponse(code = 500, message = "error in creating space object. please contact system admin")
    })
    public @ResponseBody
    ResponseEntity createSpace(@RequestBody String reqBody, @RequestHeader(value = "x-auth-login") String login, @RequestHeader(value = "x-auth-apikey") String apiKey)
    {
        Gson gson = new Gson();
        SpaceInput incomingData = gson.fromJson(reqBody, SpaceInput.class);
        if(!incomingData.isValidData())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check data");
        if(!SqlDriver.isValidApikey(login, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");
        if(SqlDriver.isDuplicateSpace(login, incomingData.name))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("this space already exists for this user");
        //TODO cross check with actual topics registered with Kafka.

        //now add this space to this user
        String topicName = "user-" + SqlDriver.getUserId(login) + "-" + incomingData.name;
        String qUserName = "user" + SqlDriver.getUserId(login) + incomingData.name;
        String qUserPass = HelperMethods.randomString(16);
        int spaceId = SqlDriver.addSpace(login, incomingData.name, qUserName, qUserPass);
        String[] kafkaTopics = KafkaClient.listTopics();
        if(Arrays.asList(kafkaTopics).contains("user" + SqlDriver.getUserId(login) + "-" + incomingData.name))
        {
            logger.info("This space " + incomingData.name + " for user: " + login + " is already with Kafka cluster.");
        }
        else
        {
            boolean status = KafkaClient.createTopic("user-" + SqlDriver.getUserId(login) + "-" + incomingData.name);
            if(status)
                logger.info("Topic registered with kafka cluster: " + "user-" + SqlDriver.getUserId(login) + "-" + incomingData.name);
            else
                logger.warn("Topic could not be registered with kafka cluster: " + "user" + SqlDriver.getUserId(login) + "-" + incomingData.name);
        }
        if(spaceId != -1)
        {
            SpaceOutput outputData = new SpaceOutput();
            outputData.id = spaceId;
            outputData.accessUrl = "/api/space/" + spaceId;
            outputData.name = incomingData.name;
            outputData.topicName = topicName;
            outputData.dataDashboardPassword = qUserPass;
            //InfluxDBClient.addDB(outputData.topicName); //just in case this has not been created by kafka topic monitoring thread
            boolean status = InfluxDBClient.addUser(outputData.topicName, outputData.topicName, outputData.dataDashboardPassword);
            if(status)
            {
                outputData.dataDashboardUser = qUserName;
                outputData.dataDashboardUrl = "http://" + AppConfiguration.getStreamAccessUrl() + "/";
            }
            else
            {
                outputData.dataDashboardPassword = null;
                logger.warn("Problem creating new user for this scape: " + outputData.topicName);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(outputData));
        }
        else
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error in creating space object. please contact system admin");
    }

    @RequestMapping(value={"/api/series/"}, method = RequestMethod.POST, produces = {"application/json"})
    @ApiOperation(value = "createSeries", notes = "Creates a new monitoring series inside an existing space")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 400, message = "check data"),
            @ApiResponse(code = 409, message = "this series already exists within the space"),
            @ApiResponse(code = 201, message = "created"),
            @ApiResponse(code = 500, message = "error in creating series object. please contact system admin")
    })
    public @ResponseBody
    ResponseEntity createSeries(@RequestBody String reqBody, @RequestHeader(value = "x-auth-login") String login, @RequestHeader(value = "x-auth-apikey") String apiKey)
    {
        Gson gson = new Gson();
        SeriesInput incomingData = gson.fromJson(reqBody, SeriesInput.class);
        if(!incomingData.isValidData())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check data");
        if(!SqlDriver.isValidApikey(login, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");
        if(SqlDriver.isDuplicateSeries(login, incomingData.name, incomingData.spaceName))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("this series already exists within the space");

        //now add this series to this user within the space
        int spaceId = SqlDriver.getSpaceId(login, incomingData.spaceName);
        if(spaceId == -1)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check data");
        }
        int seriesId = SqlDriver.addSeries(incomingData.name, incomingData.msgSignature, spaceId);

        if(seriesId != -1)
        {
            SeriesOutput outputData = new SeriesOutput();
            outputData.id = seriesId;
            outputData.accessUrl = "/api/series/" + seriesId;
            outputData.name = incomingData.name;
            return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(outputData));
        }
        else
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error in creating series object. please contact system admin");
    }

    @RequestMapping(value={"/api/pingback/"}, method = RequestMethod.POST, produces = {"application/json"})
    @ApiOperation(value = "createPingBack", notes = "Creates a new health check run for a service by a user")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 400, message = "check data"),
            @ApiResponse(code = 201, message = "created"),
            @ApiResponse(code = 500, message = "error in creating series object. please contact system admin")
    })
    public @ResponseBody
    ResponseEntity createPingBack(@RequestBody String reqBody, @RequestHeader(value = "x-auth-login") String login, @RequestHeader(value = "x-auth-apikey") String apiKey)
    {
        Gson gson = new Gson();
        HealthCheckInput incomingData = gson.fromJson(reqBody, HealthCheckInput.class);
        if(!incomingData.isValidData())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check data");
        if(!SqlDriver.isValidApikey(login, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");

        //now add this ping to this user within the db
        int pingId = SqlDriver.addPingEntry(incomingData.pingURL,incomingData.reportURL,incomingData.periodicity,incomingData.toleranceFactor,login);

        if(pingId != -1)
        {
            HealthCheckOutput outputData = new HealthCheckOutput();
            outputData.id = pingId;
            outputData.accessUrl = "/api/pingback/" + pingId;
            outputData.pingURL = incomingData.pingURL;
            outputData.reportURL = incomingData.reportURL;
            outputData.periodicity = incomingData.periodicity;
            outputData.toleranceFactor = incomingData.toleranceFactor;
            outputData.callHistory = Application.eventsCache.getEventTraceHistory(incomingData.pingURL, incomingData.reportURL);
            return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(outputData));
        }
        else
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error in creating pingback object. please contact system admin");
    }

    @RequestMapping(value={"/api/endpoint"}, method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "getEndpointInfo", notes = "Get the endpoint to send data")
    @ApiResponses({
            @ApiResponse(code = 401, message = "invalid api key"),
            @ApiResponse(code = 200, message = "ok")
    })
    public @ResponseBody
    ResponseEntity getEndpointInfo(@RequestHeader(value = "x-auth-login") String login, @RequestHeader(value = "x-auth-apikey") String apiKey) {
        Gson gson = new Gson();

        if (!SqlDriver.isValidApikey(login, apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid api key");
        //get config parameters
        EndpointInfo response = new EndpointInfo();
        response.endpoint = AppConfiguration.getKafkaURL();
        response.keySerializer = AppConfiguration.getKafkaKeySerializer();
        response.valueSerializer = AppConfiguration.getKafkaValueSerializer();
        return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(response));
    }
}

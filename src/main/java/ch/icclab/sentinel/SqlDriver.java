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
import org.apache.log4j.Logger;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.log;
import static org.jooq.impl.DSL.table;


public class SqlDriver
{
    final static Logger logger = Logger.getLogger(SqlDriver.class);
    static Connection getDBConnection()
    {
        Connection connection = null;
        if(AppConfiguration.getSentinelDBType().equalsIgnoreCase("sqlite")) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + AppConfiguration.getSentinelDBURL());
            } catch (SQLException sqex) {
                logger.error("Exception in opening Db connection: sqlite3");
                connection = null;
            }
        }
        return connection;
    }

    static ArrayList<String> getDbTablesList()
    {
        Connection con = getDBConnection();
        ArrayList<String> discoveredTableList = null;
        try {
            DatabaseMetaData metaData = con.getMetaData();
            String tableType[] = {"TABLE"};
            StringBuilder builder = new StringBuilder();
            ResultSet result = metaData.getTables(null, null, null, tableType);
            discoveredTableList = new ArrayList<String>();
            while (result.next())
            {
                discoveredTableList.add(result.getString("TABLE_NAME"));
            }
            result.close();
        } catch (java.sql.SQLException sqex)
        {
            logger.warn("Exception in getting Db table list: getDbTablesList");
            discoveredTableList = null;
        }
        return discoveredTableList;
    }

    static LinkedList<SpaceOutput> getUserSpaces(int userId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("name"), field("id"), field("queryuser"), field("querypass")).from("space").where("userid = ?").getSQL();
        LinkedList<SpaceOutput> spaces =  new LinkedList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                SpaceOutput temp = new SpaceOutput();
                temp.id = rs.getInt(2);
                temp.name = rs.getString(1);
                temp.topicName = "user-" + userId + "-" + rs.getString(1);
                temp.accessUrl = "/api/space/" + temp.id;
                temp.dataDashboardUrl = "http://" + AppConfiguration.getStreamAccessUrl() + "/";
                temp.dataDashboardUser = rs.getString(3);
                temp.dataDashboardPassword = rs.getString(4);
                temp.seriesList = getSpaceSeries(temp.id).toArray(new SeriesOutput[SqlDriver.getSpaceSeries(temp.id).size()]);
                spaces.add(temp);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in retrieving list of registered spaces for user: " + sqex.getMessage());
        }
        return spaces;
    }

    static LinkedList<SeriesOutput> getSpaceSeries(int spaceId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("name"), field("id"), field("structure")).from("series").where("spaceid = ?").getSQL();
        LinkedList<SeriesOutput> series =  new LinkedList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, spaceId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                SeriesOutput temp = new SeriesOutput();
                temp.id = rs.getInt(2);
                temp.name = rs.getString(1);
                temp.msgFormat = rs.getString(3);
                temp.accessUrl = "/api/series/" + temp.id;
                series.add(temp);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in retrieving list of registered series for user: " + sqex.getMessage());
        }
        return series;
    }

    static boolean isDuplicateUser(String login)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(DSL.count()).from("user").where("login = ?").getSQL();
        int accounts = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                accounts = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in duplicate user test." + sqex.getMessage());
        }
        if(accounts > 0) return true;
        return false;
    }

    static boolean isValidApikey(String login, String apiKey)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("apikey")).from("user").where("login = ?").getSQL();
        String apikey = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                apikey = rs.getString(1);
            }
            rs.close();
            conn.close();
            if(apikey.equalsIgnoreCase(apiKey))
                return true;

        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in validating apikey: " + sqex.getMessage());
        }
        return false;
    }

    static boolean isValidApikey(int userid, String apiKey)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("apikey")).from("user").where("id = ?").getSQL();
        String apikey = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userid);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                apikey = rs.getString(1);
            }
            rs.close();
            conn.close();
            if(apikey.equalsIgnoreCase(apiKey))
                return true;

        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in validating apikey: " + sqex.getMessage());
        }
        return false;
    }

    static boolean isValidPassword(int userid, String password)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("passwordhash")).from("user").where("id = ?").getSQL();
        String hash = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userid);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                hash = rs.getString(1);
            }
            rs.close();
            conn.close();

            if(HelperMethods.generateSHA256Hash(password).equalsIgnoreCase(hash))
                return true;
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in validating password: " + sqex.getMessage());
        }
        return false;
    }

    static boolean isDuplicateSpace(String login, String sName)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        int userId = getUserId(login);
        String sql = create.select(DSL.count()).from("space").where("name = ?").and("userid = ?").getSQL();

        int count = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sName);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                count = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in duplicate space test: " + sqex.getMessage());
        }
        if(count > 0) return true;
        return false;
    }

    static boolean isDuplicateSeries(String login, String seriesName, String spaceName)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(DSL.count()).from("series").where("name = ?").and("spaceid = ?").getSQL();
        int spaceId = getSpaceId(login, spaceName);

        int count = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, seriesName);
            stmt.setInt(2, spaceId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                count = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in duplicate series test: " + sqex.getMessage());
        }
        if(count > 0) return true;
        return false;
    }

    static int addSpace(String login, String sName, String quser, String qpass)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        int userId = getUserId(login);
        String sql = create.insertInto(table("space"), field("name"), field("queryuser"), field("querypass"), field("userid")).
                values("?", "?", "?", "?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sName);
            stmt.setString(2, quser);
            stmt.setString(3, qpass);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            id = getSpaceId(login, sName);
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in adding a new space: " + sqex.getMessage());
        }
        return id;
    }

    static int addSeries(String seriesName, String msgStructure, int spaceId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);

        String sql = create.insertInto(table("series"), field("name"), field("structure"), field("spaceid")).
                values("?", "?", "?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, seriesName);
            stmt.setString(2, msgStructure);
            stmt.setInt(3, spaceId);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            id = getSeriesId(seriesName, spaceId);
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in adding a new series: " + sqex.getMessage());
        }
        return id;
    }

    static int addPingEntry(String pingURL, String reportURL, long periodicity, int tolerance, String method, String login)
    {
        if(isDuplicatePing(login, pingURL, reportURL))
        {
            return updatePingEntry(pingURL, reportURL, periodicity, tolerance, method, getPingId(pingURL, reportURL, getUserId(login)));
        }

        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);

        String sql = create.insertInto(table("healthcheck"), field("pingurl"), field("reporturl"), field("periodicity"), field("tolerance"), field("method"), field("userid")).
                values("?", "?", "?", "?", "?", "?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, pingURL);
            stmt.setString(2, reportURL);
            stmt.setLong(3, periodicity);
            stmt.setInt(4, tolerance);
            stmt.setString(5, (method!=null ? method:"code"));
            stmt.setInt(6, getUserId(login));
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            id = getPingId(pingURL, reportURL, getUserId(login));
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in adding a new series: " + sqex.getMessage());
        }
        return id;
    }

    static int updatePingEntry(String pingURL, String reportURL, long periodicity, int tolerance, String method, int pingId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);

        String sql = create.update(table("healthcheck")).set(field("pingurl"), "?").set(field("reporturl"), "?").set(field("periodicity"), "?")
                .set(field("tolerance"), "?").set(field("method"), "?").where("id = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, pingURL);
            stmt.setString(2, reportURL);
            stmt.setLong(3, periodicity);
            stmt.setInt(4, tolerance);
            stmt.setString(5, (method!=null ? method:"code"));
            stmt.setInt(6, pingId);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            id = pingId;
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in updating an existing ping entry: " + sqex.getMessage());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return id;
    }

    static LinkedList<HealthCheckOutput> getPingList()
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("pingurl"), field("reporturl"), field("periodicity"), field("tolerance"), field("method"), field("userid"))
                .from("healthcheck").getSQL();
        LinkedList<HealthCheckOutput> pingList = new LinkedList<>();
        try
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                HealthCheckOutput data = new HealthCheckOutput();
                data.pingURL = rs.getString(1);
                data.reportURL = rs.getString(2);
                data.periodicity = rs.getLong(3);
                data.toleranceFactor = rs.getInt(4);
                data.method = rs.getString(5);
                data.id = rs.getInt(6); //abusing slightly the id filed to store userId
                pingList.add(data);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in compiling ping list: " + sqex.getMessage());
        }
        return pingList;
    }

    static LinkedList<HealthCheckOutput> getFilteredPingList(int userId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("pingurl"), field("reporturl"), field("periodicity"), field("tolerance"), field("method"), field("id"))
                .from("healthcheck").where("userid = ?").getSQL();
        LinkedList<HealthCheckOutput> pingList = new LinkedList<>();
        try
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                HealthCheckOutput data = new HealthCheckOutput();
                data.pingURL = rs.getString(1);
                data.reportURL = rs.getString(2);
                data.periodicity = rs.getLong(3);
                data.toleranceFactor = rs.getInt(4);
                data.method = rs.getString(5);
                data.id = rs.getInt(6);
                data.accessUrl = "/api/pingback/" + data.id;
                pingList.add(data);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in compiling filtered ping list: " + sqex.getMessage());
        }
        return pingList;
    }

    static HealthCheckOutput getPingData(int pingId, int userId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("pingurl"), field("reporturl"), field("periodicity"), field("tolerance"), field("method"))
                .from("healthcheck").where("id = ?").and("userid = ?").getSQL();
        HealthCheckOutput data = null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pingId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                data = new HealthCheckOutput();
                data.pingURL = rs.getString(1);
                data.reportURL = rs.getString(2);
                data.periodicity = rs.getLong(3);
                data.toleranceFactor = rs.getInt(4);
                data.method = rs.getString(5);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating ping record: " + sqex.getMessage());
        }
        return data;
    }

    static boolean isDuplicatePing(String login, String pingURL, String reportURL)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(DSL.count()).from("healthcheck").where("pingurl = ?").and("reporturl = ?").and("userid = ?").getSQL();
        int userId = getUserId(login);

        int count = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, pingURL);
            stmt.setString(2, reportURL);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                count = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in duplicate pingback object test: " + sqex.getMessage());
        }
        if(count > 0) return true;
        return false;
    }

    static int addUser(String login, String password, String apiKey)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.insertInto(table("user"), field("login"), field("passwordhash"), field("apikey")).
                values("?", "?", "?").getSQL();
        int id = -1;
        try {
            String hash = HelperMethods.generateSHA256Hash(password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, login);
            stmt.setString(2, hash);
            stmt.setString(3, apiKey);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            id = getUserId(login);
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in add user module: " + sqex.getMessage());
        }
        return id;
    }

    static int getUserId(String login)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("id")).from("user").where("login = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                id = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating id for user: " + sqex.getMessage());
        }
        return id;
    }

    static int getSpaceId(int userId, String spaceName)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("id")).from("space").where("name = ?").and("userid = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, spaceName);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                id = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating id for space: " + sqex.getMessage());
        }
        return id;
    }

    static int getSpaceId(String login, String sName)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        int userId = getUserId(login);
        String sql = create.select(field("id")).from("space").where("name = ?").and("userid = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sName);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                id = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating space-id for space: " + sqex.getMessage());
        }
        return id;
    }

    static int getSeriesId(String seriesName, int spaceId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("id")).from("series").where("name = ?").and("spaceid = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, seriesName);
            stmt.setInt(2, spaceId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                id = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating series-id for space: " + sqex.getMessage());
        }
        return id;
    }

    static int getPingId(String pingURL, String reportURL, int userId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("id")).from("healthcheck").where("pingurl = ?").and("reporturl = ?").and("userid = ?").getSQL();
        int id = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, pingURL);
            stmt.setString(2, reportURL);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                id = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating ping id for url: " + sqex.getMessage());
        }
        return id;
    }

    static String getSeriesMsgFormat(String seriesName, int spaceId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("structure")).from("series").where("name = ?").and("spaceid = ?").getSQL();
        String msgFormat = null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, seriesName);
            stmt.setInt(2, spaceId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                msgFormat = rs.getString(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating msg-format for series: " + sqex.getMessage());
        }
        return msgFormat;
    }

    static String getSeriesName(int seriesId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("name")).from("series").where("id = ?").getSQL();
        String name = null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, seriesId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                name = rs.getString(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating series name for series: " + sqex.getMessage());
        }
        return name;
    }

    static String getSpaceName(int spaceId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("name")).from("space").where("id = ?").getSQL();
        String name = null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, spaceId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                name = rs.getString(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating name for space: " + sqex.getMessage());
        }
        return name;
    }

    static int getSpaceId(int seriesId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("spaceid")).from("series").where("id = ?").getSQL();
        int spaceId = -1;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, seriesId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                spaceId = rs.getInt(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating space id for series: " + sqex.getMessage());
        }
        return spaceId;
    }

    static String getAPIKey(int userId)
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("apikey")).from("user").where("id = ?").getSQL();
        String key = null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                key = rs.getString(1);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in locating api-key for user: " + sqex.getMessage());
        }
        return key;
    }

    static LinkedList<String> getGlobalTopicsList()
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("name"), field("userid")).from("space").getSQL();
        LinkedList<String> topics =  new LinkedList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                int userId = rs.getInt(2);
                String spaceName = rs.getString(1);
                topics.add("user-" + userId + "-" + spaceName);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in retrieving list of registered spaces: " + sqex.getMessage());
        }
        return topics;
    }

    static LinkedList<HealthCheckInput> getGlobalPingList()
    {
        Connection conn = getDBConnection();
        DSLContext create = DSL.using(conn, SQLDialect.SQLITE);
        String sql = create.select(field("pingurl"), field("reporturl"), field("periodicity"), field("tolerance"), field("method")).from("healthcheck").getSQL();
        LinkedList<HealthCheckInput> pingList =  new LinkedList<>();
        try
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                HealthCheckInput temp = new HealthCheckInput();
                temp.pingURL = rs.getString(1);
                temp.reportURL = rs.getString(2);
                temp.periodicity = rs.getLong(3);
                temp.toleranceFactor = rs.getInt(4);
                temp.method = rs.getString(5);
                pingList.add(temp);
            }
            rs.close();
            conn.close();
        }
        catch(SQLException sqex)
        {
            logger.warn("Caught exception in retrieving list of registered heartbeat endpoints: " + sqex.getMessage());
        }
        logger.info("Returning list of registered health check endpoints: " + pingList.size());
        return pingList;
    }
}

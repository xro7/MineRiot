package leagueDataRetrieval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.robrua.orianna.api.core.RiotAPI;
import com.robrua.orianna.type.api.RateLimit;
import com.robrua.orianna.type.core.common.Region;
import com.robrua.orianna.type.core.match.Match;
import com.robrua.orianna.type.core.matchlist.MatchReference;
import com.robrua.orianna.type.core.summoner.Summoner;
import com.robrua.orianna.type.exception.APIException;

public class MineRiot {

	public static void main(String[] args) {
		Parser parser = new Parser("data/apikey.txt");
		List<String> apikey = parser.getData();

		MongoClient mongo = new MongoClient( "localhost" , 27017 );
		DB db = mongo.getDB("lol");
		DBCollection collection =  db.getCollection("matches");
		DBCollection collection2 =  db.getCollection("usedSummonerIds");

		Gson gson = new Gson();

		RiotAPI.setRegion(Region.EUNE);
		RiotAPI.setAPIKey(apikey.get(0));
		RiotAPI.setRateLimit(3000, 10);
		// 3,000 calls per 10 seconds AND 180,000 calls per 10 minutes
		RiotAPI.setRateLimit(new RateLimit(3000, 10), new RateLimit(180000, 600));

		Summoner summoner = RiotAPI.getSummonerByName("lee fsin");
		Long summonerID = summoner.getID();
		//Long summonerID =(long) 58065678;
		Queue<Long> summonerIds = new LinkedList<Long>();
		//Set<Long> usedIds = new HashSet<Long>();
		summonerIds.add(summonerID);

		int counter =0;


		Scanner scan= new Scanner(System.in);
		int prevLenght = 0;
		int currentLength = 0;
		do{
			if(counter%10==0){
				System.out.println("checkPoint");
				if(scan.nextInt()<0){
					break;
				}
			}
			System.out.println("iteration: "+counter+" getting match data from summoner with id: "+ summonerID);  

			if (addSummonerIdTODB(mongo, collection2, summonerID)){
				
				try{
	
				List<Long> matchIds = getMatchIdsFromSummoner(summonerID);
				System.out.println(matchIds.size()+" games were played from this summoner");
	
				List<Match> jsonMatches = getMatchesFromMatchIds(matchIds);
				System.out.println(jsonMatches.size()+" games were fetched");
	
				Set<Long> participantIds = getParticipantIds(jsonMatches);
				
				System.out.println(addMatchesTODB(mongo,collection,jsonMatches)+" added to DB");
	
				prevLenght = summonerIds.size();
				summonerIds.addAll(participantIds);
				currentLength = summonerIds.size();
				System.out.println((currentLength-prevLenght)+" users added in this iteration");
				System.out.println(summonerIds.size()+" users in queue(not unique entries)");
				}catch(NullPointerException e){
					System.out.println(e.getMessage());
					continue;
				}
			}
			counter++;
			summonerID = summonerIds.remove(); // get the head of the queue and remove it
		}while(summonerIds.size()>0);

		mongo.close();
		scan.close();


	}

	private static List<Long> getMatchIdsFromSummoner(long summonerID){
		List<Long> matchIds = new ArrayList<Long>();
		List<MatchReference> matchList;
		

			matchList = RiotAPI.getMatchList(summonerID);
			for (int i = 0; i < matchList.size(); i++) {
				try{
					matchIds.add(matchList.get(i).getID());	
				}catch(APIException e){
					System.out.println(e);
	
				}
			}


		return matchIds;

	}

	private static List<Match> getMatchesFromMatchIds(List<Long>matchIds){


		List<Match> matches = new ArrayList<Match>();
		

			for (int i = 0; i < matchIds.size(); i++) {
				try{
					matches.add(RiotAPI.getMatch(matchIds.get(i),false));
				}catch(APIException e){
					System.out.println(e);
				}
			}



		return matches;



	}

	private static Set<Long> getParticipantIds(List<Match> matches){
		Set<Long> participantIds = new HashSet<Long>();
		for (int i = 0; i < matches.size(); i++) {

			for (int j = 0; j < matches.get(i).getParticipants().size(); j++) {
				participantIds.add(matches.get(i).getParticipants().get(j).getSummonerID());
			}

		}
		return participantIds;
	}
	
	private static int addMatchesTODB(MongoClient mongo,DBCollection collection,List<Match> jsonMatches){
		
		Gson gson = new Gson();
		int counter = 0;
		for (int i = 0; i < jsonMatches.size(); i++) {
			
			try{
				DBObject dbObject = (DBObject)JSON.parse(gson.toJson(jsonMatches.get(i)));
				dbObject.put("_id", jsonMatches.get(i).getID());
				collection.insert(dbObject);
				counter++;
			}catch(DuplicateKeyException e){
				System.out.println("Duplicate entry detected");
			}catch(Exception e){
				System.out.println(e.getMessage());
				mongo.close();
				System.exit(0);
			}

		}
		return counter;
	}
	
	private static boolean addSummonerIdTODB(MongoClient mongo,DBCollection collection,Long summonerID){
		
			
			try{
				BasicDBObject dbObject = new BasicDBObject();
				dbObject.put("_id", summonerID);
				collection.insert(dbObject);
			}catch(DuplicateKeyException e){
				System.out.println("Duplicate entry detected");
				return false;
			}catch(Exception e){
				System.out.println(e.getMessage());
				mongo.close();
				System.exit(0);
			}
			return true;
	
	}

}
package leagueDataRetrieval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.Gson;
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

		Gson gson = new Gson();

		RiotAPI.setRegion(Region.EUNE);
		RiotAPI.setAPIKey(apikey.get(0));
		RiotAPI.setRateLimit(3000, 10);
		// 3,000 calls per 10 seconds AND 180,000 calls per 10 minutes
		RiotAPI.setRateLimit(new RateLimit(3000, 10), new RateLimit(180000, 600));

		Summoner summoner = RiotAPI.getSummonerByName("Gai s3ns3i");
		Long summonerID = summoner.getID();
		Set<Long> summonerIds = new HashSet<Long>();
		Set<Long> usedIds = new HashSet<Long>();
		summonerIds.add(summonerID);

		int counter =0;


		Scanner scan= new Scanner(System.in);

		do{
			if(counter%10==0){
				System.out.println("checkPoint");
				if(scan.nextInt()<0){
					break;
				}
			}
			System.out.println("iteration: "+counter+" getting match data from summoner with id: "+ summonerID);  
			usedIds.add(summonerID);

			Set<Long> matchIds = getMatchIdsFromSummoner(summonerID,"PRESEASON2017");
			System.out.println(matchIds.size()+" games");

			//List<String> matchesToJSON = new ArrayList<String>();
			List<Match> jsonMatches = getMatchesFromMatchIds(matchIds);
			Set<Long> participantIds = getParticipantIds(jsonMatches);

			for (int i = 0; i < jsonMatches.size(); i++) {
				//matchesToJSON.add(gson.toJson(jsonMatches.get(i)));
				try{
					DBObject dbObject = (DBObject)JSON.parse(gson.toJson(jsonMatches.get(i)));
					dbObject.put("_id", jsonMatches.get(i).getID());
					collection.insert(dbObject);
				}catch(DuplicateKeyException e){
					System.out.println("Duplicate entry detected");
				}catch(Exception e){
					System.out.println(e.getMessage());
					mongo.close();
					System.exit(0);
				}
				//System.out.println(gson.toJson(jsonMatches.get(i)));
			}

			summonerIds.addAll(participantIds);
			for(Long id : summonerIds){
				if(!usedIds.contains(id)){
					summonerID = id;
					break;
				}else{
					summonerID = (long) -1;
				}
			}

			counter++;     
		}while(summonerID!=-1);

		mongo.close();
		scan.close();

		//object to JSON, and assign to a String
		//String summonerString = gson.toJson(summoner);
		//System.out.println(summonerString);

	}

	private static Set<Long> getMatchIdsFromSummoner(long summonerID,String season){
		Set<Long> matchIds = new HashSet<Long>();
		List<MatchReference> matchList;
		try{
			matchList = RiotAPI.getMatchList(summonerID);
			for (int i = 0; i < matchList.size(); i++) {
				//System.out.println(matchList.get(i).getSeason());
				if(matchList.get(i).getSeason().name().equals(season)){
					matchIds.add(matchList.get(i).getID());
				}
			}
		}catch(APIException e){
			System.out.println(e);
			
		}

		return matchIds;



	}

	private static List<Match> getMatchesFromMatchIds(Set<Long>matchIds){

		List<Long> matchIdsList = new ArrayList<Long>();
		matchIdsList.addAll(matchIds);
		List<Match> matches = null;
		try{

			matches = RiotAPI.getMatches(matchIdsList);


		}catch(APIException e){
			System.out.println(e);
			System.exit(0);
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

}
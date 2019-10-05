package nearsoft.academy.bigdata.recommendation;



import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class MovieRecommender {

    private int totalReviews = 0;
    private int totalProducts = 0;
    private int totalUsers = 0;
    private float score = 0.0f;
    private BiMap<String, Integer> productsHash = HashBiMap.create();
    private Hashtable<String, Integer> usersHash = new Hashtable<>();

    public MovieRecommender(String path) throws IOException {
        writeFile(path);
    }

    private void writeFile(String path) throws IOException {
        int thisProduct =0, thisUser =0;
        Files.deleteIfExists(Paths.get("Result.csv"));
        File result = new File("Result.csv");
        InputStream fileReader = new GZIPInputStream(new FileInputStream(path));
        BufferedReader br = new BufferedReader(new InputStreamReader(fileReader));
        FileWriter fileWriter = new FileWriter(result);
        BufferedWriter bw = new BufferedWriter(fileWriter);
        String line;
        String[] sp;
        Integer identificador = 0;
        String key, value;
        while((line = br.readLine()) != null) {
            if (line.length() >= 0) {
                sp = line.split(" ");
                key = sp[0];
                if (key.equals("product/productId:")) {
                    value = sp[1];
                    if (!productsHash.containsKey(value)){
                        productsHash.put(value,identificador);
                        thisProduct = productsHash.get(value);
                        this.totalProducts++;
                    }else{
                        thisProduct = productsHash.get(value);
                    }
                }else if (key.equals("review/userId:")){
                    value = sp[1];
                    if (!usersHash.containsKey(value)){
                        usersHash.put(value, identificador);
                        thisUser = usersHash.get(value);
                        this.totalUsers++;
                    }else{
                        thisUser = usersHash.get(value);
                    }
                }else if (key.equals("review/score:")){
                    identificador ++;
                    String score = sp[1];
                    bw.write(thisUser + "," + thisProduct + "," + score + "\n");
                    this.totalReviews ++;
                }
            }
        }
        br.close();
        bw.close();
    }

    public int getTotalReviews() {
        return this.totalReviews;
    }

    public int getTotalProducts() {
        return this.totalProducts;
    }

    public int getTotalUsers() {
        return this.totalUsers;
    }

    public List<String> getRecommendationsForUser(String id) throws IOException, TasteException {
        DataModel model = new FileDataModel(new File("result.csv"));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        Long userLongId = Long.valueOf(usersHash.get(id));

        List<RecommendedItem> recommendations = recommender.recommend(userLongId, 3);
        List<String> recommendationsAsStr = new ArrayList<>();
        //productsHash.inverse();
        for (RecommendedItem recommendation: recommendations) {
            recommendationsAsStr.add((String.format("%s",productsHash.get(recommendation.getItemID()))));
        }
        return recommendationsAsStr;
    }
}

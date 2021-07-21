package com.codejoust.main.util;

import java.lang.reflect.Type;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.game_object.Submission;

public class UtilityTestMethods {

    public static String convertObjectToJsonString(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    public static <T> T toObject(String json, Class<T> c) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class,
                (JsonDeserializer<Instant>) (json1, typeOfT, context) -> Instant.parse(json1.getAsString())).create();
        return gson.fromJson(json, c);
    }

    public static <T> T toObjectType(String json, Type type) {
        Gson gson = new Gson();
        return gson.fromJson(json, type);
    }

    // Helper method to add a dummy submission to a Player object
    public static void addSubmissionHelper(Player player, int problemIndex, PlayerCode playerCode, int numCorrect) {
        Submission submission = new Submission();
        submission.setProblemIndex(problemIndex);
        submission.setNumCorrect(numCorrect);
        submission.setNumTestCases(1);
        submission.setStartTime(Instant.now());
        submission.setPlayerCode(playerCode);

        player.getSubmissions().add(submission);
        if (numCorrect == 1) {
            boolean[] solved = player.getSolved();
            solved[problemIndex] = true;
            player.setSolved(solved);
        }
    }
}

package com.example.assignment_llm;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private EditText inputp;
    private TextView outputRes;
    private ProgressBar progressBar;
    private Button Send, Cancel;
    private LLMTask llmTask;

    // Replace with your own valid Hugging Face API Key (example placeholder)
    private final String HF_API_KEY = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputp = findViewById(R.id.input_prompt);
        outputRes = findViewById(R.id.output_response);
        progressBar = findViewById(R.id.progressBar);
        Send = findViewById(R.id.btn_send);
        Cancel = findViewById(R.id.btn_cancel);

        Send.setOnClickListener(view -> {
            String prompt = inputp.getText().toString().trim();
            if (!prompt.isEmpty()) {
                llmTask = new LLMTask();
                llmTask.execute(prompt);
            } else {
                outputRes.setText("Please enter some text.");
            }
        });

        Cancel.setOnClickListener(view -> {
            if (llmTask != null) {
                llmTask.cancel(true);
                progressBar.setVisibility(View.GONE);
                outputRes.setText("Request canceled.");
            }
        });
    }

    private class LLMTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            outputRes.setText("Fetching response...");
        }

        @Override
        protected String doInBackground(String... params) {
            String prompt = params[0];
            HttpURLConnection connection = null;

            try {

                String apiUrl = "https://api-inference.huggingface.co/models/philschmid/flan-t5-base-samsum";


                // Create JSON request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("inputs", prompt);

                URL url = new URL(apiUrl);
                int attempts = 0;
                int responseCode;

                do {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Bearer " + HF_API_KEY);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // Send JSON payload
                    OutputStream os = connection.getOutputStream();
                    os.write(requestBody.toString().getBytes());
                    os.flush();
                    os.close();

                    responseCode = connection.getResponseCode();

                    // Retry on occasional 500 server errors (up to 3 times)
                    if (responseCode == 500) {
                        System.out.println("Server error (500). Retrying...");
                        Thread.sleep(3000);
                        attempts++;
                    }

                } while (responseCode == 500 && attempts < 3);

                // If the final response is not 200 (OK), return an error
                if (responseCode != 200) {
                    return "Error: API call failed (HTTP " + responseCode + ")";
                }

                // Parse the response
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                // Extract AI response from JSON
                JSONArray jsonResponse = new JSONArray(response.toString());
                return jsonResponse.getJSONObject(0).getString("generated_text");

            } catch (Exception e) {
                // Return the exception message if something goes wrong
                return "Error: " + e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            if (result != null && !result.isEmpty()) {
                outputRes.setText(result);
            } else {
                outputRes.setText("Error: No response received.");
            }
        }
    }
}

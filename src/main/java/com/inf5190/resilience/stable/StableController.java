package com.inf5190.resilience.stable;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.inf5190.resilience.CorrelationId;
import com.inf5190.resilience.model.User;

import io.github.resilience4j.retrofit.RetryCallAdapter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

@RestController
public class StableController {
    public static interface UnstableApi {
        @GET("unstable/users/{id}")
        public Call<User> getUser(@Path("id") String userId,
                @Header(CorrelationId.CORRELATION_ID) String correlationId);
    }

    private final Logger log = LoggerFactory.getLogger(StableController.class);

    private final Retry retry = Retry.of("id", RetryConfig.<Response<String>>custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(1000))
            .retryOnResult(response -> response.code() == 500)
            .failAfterMaxAttempts(true)
            .build());

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(500, TimeUnit.MILLISECONDS)
            .build();

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://127.0.0.1:8080/")
            .addCallAdapterFactory(RetryCallAdapter.of(retry))
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build();

    private final UnstableApi unstableService = retrofit.create(UnstableApi.class);

    public StableController() {
        this.retry.getEventPublisher().onRetry(e -> this.log.info("Retry attempt."));
    }

    @GetMapping("/stable/users/{id}")
    public User get(@PathVariable("id") String userId, HttpServletRequest request) {
        try {
            Response<User> r = this.unstableService
                    .getUser("userId", (String) request.getAttribute(CorrelationId.CORRELATION_ID))
                    .execute();
            if (r.code() != 200) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, r.message());
            }
            return r.body();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

}

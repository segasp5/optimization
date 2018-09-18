package ru.sberbank.optdemo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.ParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableScheduling
public class Optdemo1Application {

	public static void main(String[] args) {
		Executors.newFixedThreadPool(1).execute(() -> {
			try {
				Opdemo1Controller.quotes(1000);
			} catch (ExecutionException | InterruptedException | ParseException e) {
				e.printStackTrace();
			}
		});
		SpringApplication.run(Optdemo1Application.class, args);
	}

	@Scheduled(cron = "* 1/60 * * * *") // every day at one AM
//    @Scheduled(fixedDelay = 1000) // once a second
	public void readApplovin() {
		System.out.println("I'm alive!");
	}
}

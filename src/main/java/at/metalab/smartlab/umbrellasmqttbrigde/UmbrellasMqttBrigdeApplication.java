package at.metalab.smartlab.umbrellasmqttbrigde;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UmbrellasMqttBrigdeApplication {

	private static IMqttClient client;

	private static MqttCallback mqttCallback = new MqttCallback() {

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			String strMessage = new String(message.getPayload(), "UTF-8");

			System.out.println(topic + ": " + strMessage);

			switch (topic) {
			case "homeassistant/light/umbrellas_bridge/cmnd/POWER":
				if ("ON".equals(strMessage)) {
					setAmberWhite("255", "0");
					publish("homeassistant/light/umbrellas_bridge/POWER", "ON", true);
				} else if ("OFF".equals(strMessage)) {
					setAmberWhite("0", "0");
					publish("homeassistant/light/umbrellas_bridge/POWER", "OFF", true);
				}
				break;

			case "homeassistant/light/umbrellas_bridge/cmnd/COLOR":
				String[] rgb1 = StringUtils.split(strMessage, ",");
				setColor(rgb1[0], rgb1[1], rgb1[2]);
				publish("homeassistant/light/umbrellas_bridge/COLOR", strMessage, true);
				break;
			}

			System.out.println("done");
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
		}
	};

	private static void publish(String topic, String message, boolean retain) {
		new Thread() {
			@Override
			public void run() {
				try {
					client.publish(topic, message.getBytes(StandardCharsets.UTF_8), 0, retain);
				} catch(Exception exception) {
					exception.printStackTrace();
				}
			}
		}.start();
	}
	
	private static void setAmberWhite(String a, String w) {
		try {
			Runtime.getRuntime().exec(//
					new String[] { //
							"/home/homeassistant/.homeassistant/metalab-hacks/umbrellas/umbrella_aw.py", //
							a, w,});
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}
	
	private static void setColor(String r, String g, String b) {
		try {
			Runtime.getRuntime().exec(//
					new String[] { //
							"/home/homeassistant/.homeassistant/metalab-hacks/umbrellas/umbrella_rgb.py", //
							r, g, b });
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		String publisherId = "umbrellas_bridge";

		client = new MqttClient("tcp://10.20.30.97:1883", publisherId,
				new MqttDefaultFilePersistence("/tmp/" + publisherId));

		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName("DVES_USER");
		options.setPassword("DVES_PASS".toCharArray());
		options.setKeepAliveInterval(30);
		options.setAutomaticReconnect(true);
		options.setCleanSession(false);
		options.setConnectionTimeout(30);
		options.setWill("homeassistant/light/umbrellas_bridge/LWT", "Offline".getBytes(), 0, true);

		client.setCallback(mqttCallback);
		client.connect(options);

		client.subscribe("homeassistant/light/umbrellas_bridge/cmnd/POWER");
		client.subscribe("homeassistant/light/umbrellas_bridge/cmnd/COLOR");

		client.publish("homeassistant/light/umbrellas_bridge/LWT", "Online".getBytes(), 0, true);

		SpringApplication.run(UmbrellasMqttBrigdeApplication.class, args);
	}

}

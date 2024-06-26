package com.nike.mobileapppttpocnew.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import com.nike.mobileapppttpocnew.model.AllChannels;
import com.nike.mobileapppttpocnew.model.Channel;
import com.nike.mobileapppttpocnew.model.ChannelUsers;
import com.nike.mobileapppttpocnew.model.NotificationPayload;
import com.nike.mobileapppttpocnew.model.User;
import com.nike.mobileapppttpocnew.model.UserChannel;
import com.nike.mobileapppttpocnew.repository.ChannelRepository;
import com.nike.mobileapppttpocnew.repository.UserChannelRepository;
import com.nike.mobileapppttpocnew.repository.UserRepository;
/*import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;*/
import io.netty.handler.ssl.OpenSsl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class PTTService {

  private static final String UPLOAD_DIR = "src/main/resources/uploads";
  private UserRepository userRepository;
  private ChannelRepository channelRepository;
  private UserChannelRepository userChannelRepository;

  @Autowired
  public PTTService(UserRepository userRepository,
      ChannelRepository channelRepository, UserChannelRepository userChannelRepository) {
    this.userRepository = userRepository;
    this.channelRepository = channelRepository;
    this.userChannelRepository = userChannelRepository;
  }

  public User addUser(User user) {
    log.info("Os Name:{}",System.getProperty("os.name"));
    return userRepository.save(user);
  }

  public Channel createChannel(Channel channel) {
    return channelRepository.save(channel);
  }

  public UserChannel joinChannel(UserChannel userChannel) {
    Channel channel = channelRepository.findByUuid(userChannel.getChannelId());
    if (channel == null) {
      return null;
    }
    return userChannelRepository.save(userChannel);
  }

  public AllChannels getChannel() {
    AllChannels allChannels = new AllChannels();
    List<ChannelUsers> channelUsersList = new ArrayList<>();
    List<Channel> channelList = channelRepository.findAll();

    for(Channel channel : channelList) {
      ChannelUsers channelUsers = new ChannelUsers();
      List<Integer> userId = userChannelRepository.findBychannelId(channel.getUuid());
      List<User> userList = userRepository.findAllByIdIn(userId);
      List<UserChannel> cUserChannels = userChannelRepository.findAllByChannelId(channel.getUuid());

      for(UserChannel uChannel : cUserChannels) {
        for(User user : userList) {
          if(user.getId() == uChannel.getUserId()) {
            user.setPttToken(uChannel.getPttToken());
            userRepository.save(user);
          }
        }
      }

      channelUsers.setChannel(channel);
      channelUsers.setUsers(userList);
      channelUsersList.add(channelUsers);
    }

    allChannels.setChannels(channelUsersList);

    return allChannels;
  }

  public void leaveChannel(int id) {
    userChannelRepository.deleteById(id);
  }

  public void sendNotification(int id, String channelUuid, MultipartFile file) {
    String fileName = file.getOriginalFilename();
    NotificationPayload payload = new NotificationPayload();
    Path dirPath = Paths.get("upload");
     Path filePath;
    try {
      byte[] bytes = file.getBytes();
      
       dirPath = Paths.get("upload");
      
      if(!Files.exists(dirPath)) {
        Files.createDirectories(dirPath);
      }

       filePath = dirPath.resolve(fileName);
       payload.setAudioUrl("/mobileapp/ptt/get-audio/"+fileName);

      System.out.println("dirctory:"+filePath+" file:"+fileName);
      Files.write(filePath, bytes);
    } catch (IOException e) {
      log.error(e.getMessage());
    }

    Optional<User> byId = userRepository.findById(id);

    Channel channel = channelRepository.findByUuid(channelUuid);

    payload.setUserName(byId.get().getName());

    List<UserChannel> allByChannelId = userChannelRepository.findAllByChannelId(channelUuid);

    for (UserChannel userChannel : allByChannelId) {
      ApnsClient service = getApns();

    final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
    payloadBuilder.setAlertBody(payload.toString());

    final String notifPayload = payloadBuilder.build();
      if(userChannel.getUserId() != byId.get().getId()) {
        System.out.println(userChannel.getUserId());
      SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(TokenUtil.sanitizeTokenString(userChannel.getPttToken()),
          "com.nike.pushToTalk.voip-ptt", notifPayload, Instant.now(), DeliveryPriority.IMMEDIATE, PushType.PUSH_TO_TALK);
          System.out.println(pushNotification.toString());
      service.sendNotification(pushNotification);
      }
    }

  }

  public List<User> getUsers(String channelId) {
    List<Integer> userIds = userChannelRepository.findBychannelId(channelId);
    List<User> users = userRepository.findAllByIdNotIn(userIds);
    return users;
  }

 public void sendOneToOneNotification(int id1, int id2, String message) {
    Optional<User> userbyId1 = userRepository.findById(id1);
    Optional<User> userbyId2 = userRepository.findById(id2);
    Optional<UserChannel> userChannelbyId = userChannelRepository.findById(id1);

    NotificationPayload payload = new NotificationPayload();
    payload.setMessage(message);
    payload.setChannelName(channelRepository.findByUuid(userChannelbyId.get().getChannelId()).getChannelName());
    payload.setChannelUuid(userChannelbyId.get().getChannelId());
    payload.setUserName(userbyId1.get().getName());

    ApnsClient service = getApns();


    final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
    payloadBuilder.setAlertBody(payload.toString());

    final String notifPayload = payloadBuilder.build();

    SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(TokenUtil.sanitizeTokenString(
        userbyId2.get().getDeviceToken()),
        "com.nike.pushToTalk", notifPayload);
   PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendPushNotification = service.sendNotification(
       pushNotification);
    try {
      PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendPushNotification.get();
      if (pushNotificationResponse.isAccepted()) {
        System.out.println("Push notification accepted by APNs gateway successfully.");
      } else {
        System.out.println("Notification rejected by the APNs gateway: " +
            pushNotificationResponse.getRejectionReason());

        pushNotificationResponse.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
          System.out.println("\t… the token is invalid as of " + timestamp);
        });
      }
    } catch (final ExecutionException | InterruptedException e) {
      System.err.println("Failed to send push notification.");
      e.printStackTrace();
    }


 }

/*  public void sendOneToOneNotification(int id1, int id2, String message) {
    Optional<User> userbyId1 = userRepository.findById(id1);
    Optional<User> userbyId2 = userRepository.findById(id2);
    Optional<UserChannel> userChannelbyId = userChannelRepository.findById(id1);

    NotificationPayload payload = new NotificationPayload();
    payload.setMessage(message);
    payload.setChannelName(channelRepository.findByUuid(userChannelbyId.get().getChannelId()).getChannelName());
    payload.setChannelUuid(userChannelbyId.get().getChannelId());
    payload.setUserName(userbyId1.get().getName());

    ApnsService service = getApnsService();

    String stringPayload = APNS.newPayload().alertTitle("OnetoOneNotif").alertBody(payload.toString()).build();
    String token = userbyId2.get().getDeviceToken();
    service.push(token,stringPayload);

  }*/

  public ApnsClient getApns() {
    System.out.println("OpenSSL available? " + OpenSsl.isAvailable());
    System.out.println("ALPN supported?    " + OpenSsl.isAlpnSupported());

    if (OpenSsl.unavailabilityCause() != null) {
      System.out.println("Reason for unavailability:");
      OpenSsl.unavailabilityCause().printStackTrace(System.out);
    }

    ApnsClient apnsClient;
    try {
       apnsClient = new ApnsClientBuilder()
          .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST, ApnsClientBuilder.ALTERNATE_APNS_PORT)
          .setClientCredentials(new File("src/main/resources/AthleteCommunication-Dev.p12"), "Nike1234!")
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return apnsClient;
  }

  /*public ApnsService getApnsService() {
    return APNS.newService()
        .withCert("src/main/resources/AthleteCommunication-Dev.p12","Nike1234!")
        .withProductionDestination().build();
  }*/
}

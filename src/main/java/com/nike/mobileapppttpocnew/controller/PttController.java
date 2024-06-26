package com.nike.mobileapppttpocnew.controller;

import com.nike.mobileapppttpocnew.model.AllChannels;
import com.nike.mobileapppttpocnew.model.Channel;
import com.nike.mobileapppttpocnew.model.User;
import com.nike.mobileapppttpocnew.model.UserChannel;
import com.nike.mobileapppttpocnew.service.PTTService;

import java.io.File;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/mobileapp/ptt")
public class PttController {

  private PTTService pttService;

  @Autowired
  public PttController(PTTService pttService) {
    this.pttService = pttService;
  }


  @PostMapping("/sendNotification")
  public ResponseEntity<?> sendNotification(@RequestParam("userId") int id,
      @RequestParam("channelUuid") String channelUuid, @RequestParam("file") MultipartFile file) {
    pttService.sendNotification(id, channelUuid, file);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/createUser")
  public ResponseEntity<?> addUser(@RequestBody User user) {
    Optional<User> userRes = Optional.ofNullable(pttService.addUser(user));
    if (userRes.isPresent()) {
      return new ResponseEntity<>(userRes.get(), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/createChannel")
  public ResponseEntity<?> createChannel(@RequestBody Channel channel) {
    Optional<Channel> channelRes = Optional.ofNullable(pttService.createChannel(channel));
    if (channelRes.isPresent()) {
      return new ResponseEntity<>(channelRes.get(), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/joinChannel")
  public ResponseEntity<String> joinChannel(@RequestBody UserChannel userChannel) {
    Optional<UserChannel> channelRes = Optional.ofNullable(pttService.joinChannel(userChannel));
    if (channelRes.isPresent()) {
      return new ResponseEntity<>(HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/oneToOneNotification")
  public ResponseEntity<String> sendOneToOneNotification(@RequestParam("userId1") int userId1,
      @RequestParam("userId2") int userId2,
      @RequestBody String message) {
    pttService.sendOneToOneNotification(userId1, userId2, message);
    return new ResponseEntity<>("Message Sent", HttpStatus.OK);
  }

  @GetMapping("/allChannels")
  public ResponseEntity<AllChannels> getChannels() {
    Optional<AllChannels> listChannel = Optional.ofNullable(pttService.getChannel());
    return listChannel.map(channels -> new ResponseEntity<>(channels, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @DeleteMapping("/leaveChannel")
  public ResponseEntity<String> leaveChannel(@RequestParam("userId") int userId) {
    pttService.leaveChannel(userId);
    return new ResponseEntity<>("User has left the channel", HttpStatus.OK);
  }


  @GetMapping("/fetchUsers")
  public ResponseEntity<List<User>> getUsers(@RequestParam("channelId") String channelId) {
    Optional<List<User>> listUsers = Optional.ofNullable(pttService.getUsers(channelId));
    return listUsers.map(users -> new ResponseEntity<>(users, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @GetMapping("/get-audio/{fileName}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable("fileName") String fileName) {
        try {
            File file = new File("upload" +"/"+ fileName);
            if (file.exists()) {
                Resource resource = new FileSystemResource(file);

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; "+fileName+"");

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}

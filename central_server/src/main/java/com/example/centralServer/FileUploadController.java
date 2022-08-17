package com.example.centralServer;


import com.example.centralServer.storage.ServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
//lock
import com.example.centralServer.redisLock.RedisLockTool;

import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileUploadController {
    ServerManager serverManager;
    RedisLockTool lockManager;

    @Autowired
    public FileUploadController(ServerManager serverManager) {
        this.serverManager = serverManager;
//        this.lockManager = new RedisLockTool();
    }

    /**
     * get all the files and directories under a directory
     *
     * @param path path of the directory
     * @return A List of String with the name of paths and directories
     */
    @GetMapping("/dirs")
    @ResponseBody
    public ResponseEntity<?> getDirectoriesByPath(@RequestParam(value = "path", defaultValue = "/") String path) {
    	String lockKey = path;
    	String identity = "getLock";
    	boolean lockSuccess = lockManager.getLock(lockKey, identity, 60);
    	
    	if (lockSuccess) {
    	String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();
        
        //release lock
        boolean releaseSuccess = (boolean) lockManager.unlock(lockKey, identity);
        return restTemplate.getForEntity(url + "/dirs?path=" + path, String.class);
    	}else {
    		return ResponseEntity.status(HttpStatus.CONFLICT).body("Directory is bing modified");
    	}
    }

    /**
     * create a directory by path
     *
     * @param path path of the new directory
     * @return response
     */
    @PostMapping("/dirs")
    @ResponseBody
    public ResponseEntity<?> createDirectoryByPath(@RequestParam(value = "path") String path) {
        String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForEntity(url + "/dirs?path=" + path, null, String.class);
    }


    /**
     * delete a directory by path
     *
     * @param path path of the directory
     * @return response
     */
    @DeleteMapping("/dirs")
    @ResponseBody
    public void deleteDirectoryByPath(@RequestParam(value = "path") String path) {
        //lock
    	String lockKey = path;
    	String identity = "deltetLock";
    	boolean lockSuccess = lockManager.getLock(lockKey, identity, 60);
    	 
    	if (lockSuccess) {
    	String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(url + "/dirs?path=" + path);
        //release lock
        boolean releaseSuccess = (boolean) lockManager.unlock(lockKey, identity);
    	 }
    }

    /**
     * get a file by path
     *
     * @param path path of the file
     * @return file
     */
    @GetMapping("/files")
    @ResponseBody
    public ResponseEntity<Resource> getFileByPath(@RequestParam(value = "path") String path) {
        String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForEntity(url + "/files?path=" + path, Resource.class);
    }

    /**
     * upload a file in a multipart form
     *
     * @param path path of the file
     * @return response
     */
    @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> handleFileUpload(@RequestBody MultipartFile file, @RequestParam("path") String path) throws IOException {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", file.getResource());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();

        String result = restTemplate.postForObject(url + "/files?path=" + path, requestEntity, String.class);
        return ResponseEntity.ok(result);
    }


    /**
     * delete a file by path
     *
     * @param path path of the file
     */
    @DeleteMapping("/files")
    @ResponseBody
    public void deleteFileByPath(@RequestParam(value = "path") String path) {
        String url = serverManager.getNextAvailableServerUrl();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(url + "/files?path=" + path);
    }

}

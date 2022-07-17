# CS6650-distributed-cloud-storage-system

### Project Name: Distributed Cloud Storage System
### Team Members: Jialun Chen, Meiqing Pan, Yun Feng

## 1. Summary description of the project use case

Our team will design a distributed storage system like Google Drive or Dropbox with a terminal client interface. Feature expectations are listed below:

Functional Requirements:
- Users should be able to signup and log in;
- Users can interact with the system through terminals;
- The system should support different users with different files;
- Users can upload, edit, and delete files;
- Users can view files catalogs;
- Users can download files;
- Files should be synchronized in all nodes in the distributed system;

Nonfunctional Requirements:
- Performance: Service should handle hundreds of users at the same time.
- Availability: Service needs to be highly available;
- Reliability: Data should be replicated to tolerate server failure.
- Scalability: The system should scale with the addition of new servers.

## 2. Architecture overview diagram (AOD) and design description

The system is composed of clients, a central server, file servers, metadata storage, and file storage.
A client can signup, login, upload/edit/delete/view files.
For the server side, we need a central server for client interaction and authentication, one metadata storage to store structured data, such as file size and path, and some file server and storage for upload, editing, deleting, and searching file data.
Memory caching is also an option for the system to boost performance.

System APIs:
- save(File file, User user)
- update(File file, User user)
- delete(File file, User user)
- view(User user)
- download(File file, User user)

## 3. Implementation Approach (high-level design)

### 1. Client

Because files need to be transferred from client to the central server, as well as requests to browse, delete, download and upload files, HTTP will be used in this part.

Apache HTTP Client library is used in the client as it has some important features:
- Supports synchronize and asynchronize requests.
- Cookies. In our project, cookies can be used to maintain user login
session
- Authentication. Apache HTTP client supports multiple methods like
Digest, NTLM and SPNEGO, which will be used to authenticate users in
our project.
- Compression. Apache HTTP client supports GZip and Deflate
compression method. File compression can increase the throughput of
our file system.
- Caching. Caching is supported in Apache ATTP client. Caching will
decrease the number of requests needed, thus decreasing the workload of the whole system

### 2. Central server
The features of the central server are shown below:
-Listen for requests from clients. For GET requests, the central server evenly distributes them to other servers, and the server that received this request would send the data to the client. For requests that modify existing data, it uses two-phase commit protocol(2PC) to make sure the data is consistent among all servers.
- Invoke the node server. The central server can invoke the node server using RPC to send data back to the client or update data. The actual work is done in different node servers so that the system is highly scalable and the throughput is greatly increased.

- Authenticate users and store user data in a database. In our project, users have separate file storage.
Java.sql library is used to connect to our PostgreSQL database and update user data.
- Manage servers. The central server can add and remove other servers. If a server is determined to be down, it will be removed from the system and stop distributing requests to it.
- Log system information. The central server would log the state of the whole system and each server, it can also record the requests received.
- Recover nodes from crash. The central server is able to recover nodes from crash.

### 3. Node server
The features of the node server are shown below:
- Send data to clients. The node server can send files and directory information back to clients。
- Invoked by the central server using RPC.
- Recover from crash

## 4. What libraries will use and how will you implement the project
- Database: PostgreSQL, java.sql
- Cache and Distributed Lock Manager: Redis
- Proxy server: Apache
- RPC: Org.apache.xmlrpc
- Send HTTP request: Apache HTTP Client

## 5. Key algorithms involved (must include at least 4 significant algorithms we covered.)

- Distributed Mutual Exclusion;
A distributed mutual exclusion solution is key to our distributed system, especially to guarantee safety and liveness. For example, concurrent updates on the same file should be protected by a mutex lock.

- PAXOS;
We will be using PAXOS for leader election in our system. Our system
consists of multiple static data stores and each data store will have the same replicated data for high availability. In order to do that, we need to have a master node and we will need an election mechanism.

- Group Communication;
We will be using different types of group communications as they are required for implementing PAXOS 

- Fault tolerance;
We will be using a fault-tolerant two-phase commit to make sure all replicated file stores have the same replicas.

## 6. Expected Results
The distributed system will use Mutual Exclusion, PAXOS, Group Communication, and Fault tolerance to implement a cloud file storage service.

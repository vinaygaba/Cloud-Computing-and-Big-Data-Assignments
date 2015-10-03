import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

public class HW1 {
	
	static AmazonEC2 ec2;
	
	static AWSCredentials credentials;
	static String keyName;
	static String securityGroupName;
	static int randomNumber = 0;
	
	
	public static void main(String[] args) throws Exception {

        System.out.println("===========================================");
        System.out.println("Assignment 1");
        System.out.println("Name: Vinay Gaba");
        System.out.println("UID: vhg2105");
        System.out.println("===========================================");

        init();
        getRandomNumber();
        securityGroupName = createSecurityGroup(Constants.SECURITY_GROUP_NAME + randomNumber);
        keyName = createKeyPair(Constants.KEY_PAIR_NAME + randomNumber).getKeyName();
        createInstance("ami-e3106686",InstanceType.T2_MICRO,1,1,keyName,securityGroupName);
        
	}
	
	
	private static void init() throws Exception {

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (/Users/vinaygaba/.aws/credentials).
         */
        credentials = null;
        try {
            //credentials = new ProfileCredentialsProvider("default").getCredentials();
        	
        	/*
        	 * Please update the AwsCredentials.properties file with your accessKey and secretKey
        	 */
        	credentials = new PropertiesCredentials(
       			HW1.class.getResourceAsStream("AwsCredentials.properties"));
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location, and is in valid format.",
                    e);
        }
        
        ec2 = new AmazonEC2Client(credentials);
        
    }
	 
	 
	 /**********************************
	  * Method to create a new Key Pair
	  * 
	  **********************************/
	 public static KeyPair createKeyPair(String keyName){
		 
		 	System.out.println("**** Creating KeyPair Group ****");
	    	CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
	    	createKeyPairRequest.withKeyName(keyName);
	    	CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
	    	
	 
	    	KeyPair keyPair = new KeyPair();
	    	keyPair = createKeyPairResult.getKeyPair();
	    	String privateKey = keyPair.getKeyMaterial();
	    	
	    	System.out.println("Key Created with Name: " + keyPair.getKeyName());
	    	writeKeyToPem(keyPair.getKeyMaterial(),keyName);
	    	
	    	return keyPair;	
	    }
	 
	 
	 /********************************************************************
	  * Method to write the created key to a .pem file
	  ********************************************************************/
	 public static void writeKeyToPem(String key,String keyName){
		 
		 try {
				File file = new File(keyName+".pem");
				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}

				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(key);
				bw.close();
				System.out.println("**** Key saved as pem file ****");

			} catch (IOException e) {
				e.printStackTrace();
			}
	 }
	
	 /******************************************
	  * Method to create a new Security Group
	  ******************************************/
	 public static String createSecurityGroup(String groupName){
	    	
		 	System.out.println("**** Creating Security Group ****");
	    	
		 	CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
	    	csgr.withGroupName(groupName).withDescription("Security Group");
	    	CreateSecurityGroupResult createSecurityGroupResult = ec2.createSecurityGroup(csgr);
	    			
	    	System.out.println("Security Group Created with name " + csgr.getGroupName());
	    	
	    	createIPPermission(csgr.getGroupName());
	    	
	    	
	    	return csgr.getGroupName();
	    }
	 
	 
	 /***********************************
	  * Method to create IP Permissions
	  ***********************************/
	 public static void createIPPermission(String groupname){
	    	
	    	List<IpPermission> ipPermissions = createIPPermissions();
	    		
	    	AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
	    				new AuthorizeSecurityGroupIngressRequest();
	    			    	
	    	authorizeSecurityGroupIngressRequest.withGroupName(groupname)
	    			                            .withIpPermissions(ipPermissions);
	    			
	    	ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
	    			
	    }
	 
	 
	 /*********************************************************************
	  * Method to create IP Permissions for ssh, http and https
	  *********************************************************************/
	 public static List<IpPermission> createIPPermissions(){
		 
		 System.out.println("**** Creating IPPermissions ****");
		 
		 List<IpPermission>	ipPermissionList = new ArrayList<IpPermission>();
		 
		 IpPermission ssh = new IpPermission().withIpRanges("0.0.0.0/0").withIpProtocol("tcp").withFromPort(22).withToPort(22);
		 IpPermission http = new IpPermission().withIpRanges("0.0.0.0/32","255.255.255.255/32").withIpProtocol("tcp").withFromPort(80).withToPort(80);
		 IpPermission https = new IpPermission().withIpRanges("0.0.0.0/32","255.255.255.255/32").withIpProtocol("tcp").withFromPort(443).withToPort(443);
		 
		 ipPermissionList.add(ssh);
		 ipPermissionList.add(http);
		 ipPermissionList.add(https);
		 
		 System.out.println("**** IPPermissions Creted ****");
		 
		 return ipPermissionList;
		
	 	}
	 
	 
	 /*******************************************************************************************************
	  * Method to create a new Instance
	 
	  Example : runInstancesRequest.withImageId("ami-4b814f22")
			                       .withInstanceType("t2.micro")
			                       .withMinCount(1)
			                       .withMaxCount(1)
			                       .withKeyName("my-key-pair")
			                       .withSecurityGroups("my-security-group");
	  *******************************************************************************************************/
	
	 public static void createInstance(String imageID, String instanceType, int minCount, int maxCount,String keyName,String securityGroupName){
		 
		 System.out.println("**** Creating Instance ****");
		 RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
			        	
		 runInstancesRequest.withImageId(imageID)
			                .withInstanceType(instanceType)
			                .withMinCount(minCount)
			                .withMaxCount(maxCount)
			                .withKeyName(keyName)
			                .withSecurityGroups(securityGroupName);
			  
		 RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
		 
		 System.out.println("**** Instance Created ****");
		 
		 //getIPAddressForCurrentInstances(runInstancesResult);
		 
		 //Added wait period so that the instance is started before looking for its IP Address
		 try{
			 Thread.sleep(300000);
		 }
		 catch(Exception e){
			 Thread.interrupted();
		 }
		 
		 
		 getIPAddressForAllInstances();
	 }
	
	 
	 /***********************************
	  * Method to stop an instance
	  ***********************************/
	 public static void stopInstances(List<String> instanceIds){
		 
		 StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
         ec2.stopInstances(stopIR);
	 }

	
	 /***********************************
	  * Method to terminate an instance
	  ***********************************/
	 public static void terminateInstances(List<String> instanceIds){
		 
		 TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
         ec2.terminateInstances(tir);
		 
	 }
	 
	 
	 /*******************************************************************************************************
	  * Method to get Public and Private IP Address of current instances that were last created
	  *******************************************************************************************************/
	 public static void getIPAddressForCurrentInstances(RunInstancesResult runInstancesResult){
		 
		 
		 List<Instance> instances = runInstancesResult.getReservation().getInstances();
		
         
         for(Instance instance:instances){
        	System.out.println("**** Instance ID ****" + instance.getInstanceId());
        	System.out.println("Public IP Address: " + instance.getPublicIpAddress());
        	System.out.println("Private IP Address: " + instance.getPrivateIpAddress());
         }
	 }
	 
	 
	 /*********************************************************************
	  * Method to get Public and Private IP Address of all instances 
	  *********************************************************************/
	 public static void getIPAddressForAllInstances(){
		 
		 DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
		 List<Reservation> reservations = describeInstancesRequest.getReservations();
		 Set<Instance> instances = new HashSet<Instance>();

         for (Reservation reservation : reservations) {
             instances.addAll(reservation.getInstances());
             
         }
         
         for(Instance instance:instances){
        	System.out.println("**** Instance ID ****" + instance.getInstanceId());
        	System.out.println("Public IP Address: " + instance.getPublicIpAddress());
        	System.out.println("Private IP Address: " + instance.getPrivateIpAddress());
         }
	 }
	 
	 public static void getRandomNumber(){
		 Random random = new Random();
	     randomNumber = random.nextInt(1000);
	    
	 }
	 
	 
}

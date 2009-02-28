DROP TABLE  ACCOUNTS;
CREATE TABLE  ACCOUNTS 
   (    ACCOUNTID VARCHAR(50)  NOT NULL , 
    PASSWORD VARCHAR(50)  NOT NULL , 
    MANAGER VARCHAR(50), 
    MIDDLENAME VARCHAR(50) NULL, 
    FIRSTNAME VARCHAR(50) NOT NULL, 
    LASTNAME VARCHAR(50) NOT NULL, 
    EMAIL VARCHAR(250) NULL, 
    DEPARTMENT VARCHAR(250) NULL, 
    TITLE VARCHAR(250) NULL, 
    AGE INTEGER NULL, 
    SALARY NUMERIC(9,2) NULL, 
    JPEGPHOTO BLOB NULL, 
     PRIMARY KEY (ACCOUNTID)
   );


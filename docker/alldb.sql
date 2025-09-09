-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: lib_manage
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `book_info`
--

DROP TABLE IF EXISTS `book_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `book_name` varchar(127) NOT NULL,
  `author` varchar(127) NOT NULL,
  `price` decimal(7,2) NOT NULL,
  `publish` varchar(256) NOT NULL,
  `donor_id` int NOT NULL,
  `status` tinyint DEFAULT '1' COMMENT '0-无效，1-正常，2-不允许借阅',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_donor` (`donor_id`),
  CONSTRAINT `fk_donor` FOREIGN KEY (`donor_id`) REFERENCES `user_info` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_info`
--

LOCK TABLES `book_info` WRITE;
/*!40000 ALTER TABLE `book_info` DISABLE KEYS */;
INSERT INTO `book_info` VALUES (1,'Java编程思想','Bruce Eckel',88.00,'机械工业出版社',1,1,'2025-08-13 21:05:08','2025-08-13 21:05:08'),(2,'深入理解计算机系统','Randal E. Bryant',99.50,'人民邮电出版社',1,1,'2025-08-13 21:05:51','2025-08-14 00:09:55'),(3,'算法导论','Thomas H. Cormen',120.00,'清华大学出版社',1,1,'2025-08-13 21:06:08','2025-08-13 21:06:08'),(4,'代码大全','Steve McConnell',98.00,'电子工业出版社',1,1,'2025-08-13 21:06:56','2025-08-13 21:06:56'),(5,'计算机网络','谢希仁',59.00,'电子工业出版社',1,1,'2025-08-13 21:07:04','2025-08-14 00:51:29'),(6,'操作系统概念','Abraham Silberschatz',110.00,'机械工业出版社',1,1,'2025-08-13 21:07:09','2025-08-14 00:57:49'),(7,'深入理解Java虚拟机','周志明',88.00,'机械工业出版社',1,1,'2025-08-13 21:07:14','2025-08-14 01:52:56'),(8,'深入理解Java虚拟机','周志明',108.00,'机械工业出版社',1,1,'2025-08-13 21:07:32','2025-08-14 01:45:50'),(9,'数据库及其基本原理','author1',44.00,'undefined',1,1,'2025-08-14 01:04:57','2025-08-14 01:04:57'),(10,'毛泽东思想和中国特色社会主义理论体系概论','author2',34.00,'undefined',2,1,'2025-08-14 01:53:40','2025-08-14 01:53:40');
/*!40000 ALTER TABLE `book_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_history`
--

DROP TABLE IF EXISTS `borrow_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_history` (
  `book_id` int NOT NULL,
  `user_id` int NOT NULL,
  `borrow_time` datetime NOT NULL,
  PRIMARY KEY (`user_id`,`book_id`,`borrow_time`),
  KEY `fk_history_book` (`book_id`),
  CONSTRAINT `fk_history_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`),
  CONSTRAINT `fk_history_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_history`
--

LOCK TABLES `borrow_history` WRITE;
/*!40000 ALTER TABLE `borrow_history` DISABLE KEYS */;
INSERT INTO `borrow_history` VALUES (2,1,'2025-08-14 00:09:07'),(5,1,'2025-08-14 00:51:25'),(6,1,'2025-08-14 00:51:23'),(6,1,'2025-08-14 00:57:47'),(7,2,'2025-08-14 01:52:43'),(8,1,'2025-08-14 00:44:35'),(8,1,'2025-08-14 00:51:55'),(8,1,'2025-08-14 01:09:38'),(8,1,'2025-08-14 01:15:01');
/*!40000 ALTER TABLE `borrow_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_info`
--

DROP TABLE IF EXISTS `borrow_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_info` (
  `book_id` int NOT NULL,
  `user_id` int NOT NULL,
  `borrow_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `due_time` datetime GENERATED ALWAYS AS ((`borrow_time` + interval 45 day)) STORED,
  PRIMARY KEY (`book_id`,`user_id`),
  KEY `idx_user_due` (`user_id`,`due_time`),
  CONSTRAINT `fk_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`),
  CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_info`
--

LOCK TABLES `borrow_info` WRITE;
/*!40000 ALTER TABLE `borrow_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `borrow_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_name` varchar(128) NOT NULL,
  `password_hash` varchar(256) NOT NULL,
  `delete_flag` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_name_UNIQUE` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_info`
--

LOCK TABLES `user_info` WRITE;
/*!40000 ALTER TABLE `user_info` DISABLE KEYS */;
INSERT INTO `user_info` VALUES (1,'ljx','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',0,'2025-08-12 23:59:18','2025-08-12 23:59:18'),(2,'刘家炫','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',0,'2025-08-13 22:29:13','2025-08-13 22:29:13');
/*!40000 ALTER TABLE `user_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-05 17:01:54

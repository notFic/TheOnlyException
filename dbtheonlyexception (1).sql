-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 10, 2025 at 06:58 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dbtheonlyexception`
--

-- --------------------------------------------------------

--
-- Table structure for table `game_session`
--

CREATE TABLE `game_session` (
  `session_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `survival_time` int(11) NOT NULL,
  `total_dmg_inflicted` int(11) NOT NULL,
  `kills` int(11) NOT NULL,
  `session_date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `game_session`
--

INSERT INTO `game_session` (`session_id`, `player_id`, `survival_time`, `total_dmg_inflicted`, `kills`, `session_date`) VALUES
(1, 1, 14, 0, 0, '2025-04-22'),
(2, 1, 22, 0, 0, '2025-04-22'),
(3, 1, 14, 0, 0, '2025-04-22'),
(4, 1, 18, 0, 0, '2025-04-22'),
(5, 1, 18, 0, 0, '2025-04-22'),
(6, 1, 12, 0, 0, '2025-04-22'),
(7, 1, 50, 0, 0, '2025-04-22'),
(8, 1, 19, 0, 0, '2025-04-22'),
(9, 1, 205, 22206, 273, '2025-04-25'),
(10, 1, 262, 32060, 347, '2025-04-26'),
(11, 1, 175, 21394, 215, '2025-04-26'),
(12, 1, 78, 6698, 72, '2025-04-26'),
(13, 1, 266, 30665, 327, '2025-04-26'),
(14, 1, 40, 3962, 38, '2025-04-27'),
(15, 1, 15, 1032, 8, '2025-04-27'),
(16, 1, 65, 6584, 66, '2025-04-27'),
(17, 1, 255, 27681, 302, '2025-04-28'),
(18, 1, 255, 27681, 302, '2025-04-28'),
(19, 1, 29, 2912, 28, '2025-04-28'),
(20, 1, 29, 2912, 28, '2025-04-28'),
(21, 1, 23, 2114, 19, '2025-04-28'),
(22, 1, 23, 2114, 19, '2025-04-28'),
(23, 1, 20, 1635, 17, '2025-04-28'),
(24, 1, 20, 1635, 17, '2025-04-28'),
(25, 1, 19, 1445, 16, '2025-04-28'),
(26, 1, 19, 1445, 16, '2025-04-28'),
(27, 1, 34, 3200, 32, '2025-04-28'),
(28, 1, 34, 3200, 32, '2025-04-28'),
(29, 1, 23, 1658, 17, '2025-04-28'),
(30, 1, 23, 1658, 17, '2025-04-28'),
(31, 1, 17, 1184, 11, '2025-04-28'),
(32, 1, 17, 1184, 11, '2025-04-28'),
(33, 2, 40, 3076, 31, '2025-04-28'),
(34, 2, 40, 3076, 31, '2025-04-28'),
(35, 1, 27, 2420, 26, '2025-04-28'),
(36, 1, 27, 2420, 26, '2025-04-28'),
(37, 1, 11, 715, 10, '2025-04-28'),
(38, 1, 11, 715, 10, '2025-04-28'),
(39, 1, 7, 420, 6, '2025-04-28'),
(40, 1, 7, 420, 6, '2025-04-28'),
(41, 1, 37, 4653, 53, '2025-04-30'),
(42, 1, 9, 621, 6, '2025-04-30'),
(43, 1, 7, 304, 5, '2025-04-30'),
(44, 1, 10, 693, 8, '2025-04-30'),
(45, 1, 18, 1809, 22, '2025-04-30'),
(46, 1, 18, 1809, 22, '2025-04-30'),
(47, 1, 6, 322, 5, '2025-04-30'),
(48, 1, 12, 1325, 15, '2025-04-30'),
(49, 1, 28, 2873, 38, '2025-04-30'),
(50, 1, 28, 2873, 38, '2025-04-30'),
(51, 1, 48, 4121, 33, '2025-04-30'),
(52, 1, 48, 4121, 33, '2025-04-30'),
(53, 1, 16, 1652, 19, '2025-04-30'),
(54, 1, 16, 1652, 19, '2025-04-30'),
(55, 1, 77, 9499, 92, '2025-05-08'),
(56, 1, 77, 9499, 92, '2025-05-08'),
(57, 3, 196, 20493, 164, '2025-05-08'),
(58, 3, 196, 20493, 164, '2025-05-08'),
(59, 1, 51, 6529, 63, '2025-05-08'),
(60, 1, 51, 6529, 63, '2025-05-08'),
(61, 1, 13, 1558, 14, '2025-05-08'),
(62, 1, 13, 1558, 14, '2025-05-08'),
(63, 3, 13, 982, 14, '2025-05-08'),
(64, 3, 13, 982, 14, '2025-05-08'),
(65, 1, 52, 7029, 72, '2025-05-09'),
(66, 1, 52, 7029, 72, '2025-05-09'),
(67, 1, 81, 11241, 109, '2025-05-09'),
(68, 1, 81, 11241, 109, '2025-05-09'),
(69, 3, 28, 4010, 36, '2025-05-09'),
(70, 3, 28, 4010, 36, '2025-05-09'),
(71, 3, 74, 10259, 97, '2025-05-09'),
(72, 3, 74, 10259, 97, '2025-05-09'),
(73, 4, 24, 2522, 23, '2025-05-09'),
(74, 4, 24, 2522, 23, '2025-05-09'),
(75, 5, 431, 41290, 319, '2025-05-09'),
(76, 5, 431, 41290, 319, '2025-05-09'),
(77, 1, 69, 10255, 98, '2025-05-09'),
(78, 1, 69, 10255, 98, '2025-05-09'),
(79, 4, 69, 9330, 90, '2025-05-09'),
(80, 4, 69, 9330, 90, '2025-05-09'),
(81, 4, 21, 2380, 24, '2025-05-09'),
(82, 4, 21, 2380, 24, '2025-05-09'),
(83, 1, 16, 1960, 20, '2025-05-09'),
(84, 1, 16, 1960, 20, '2025-05-09'),
(85, 1, 19, 2932, 34, '2025-05-09'),
(86, 1, 19, 2932, 34, '2025-05-09'),
(87, 1, 21, 3336, 39, '2025-05-10'),
(88, 1, 21, 3336, 39, '2025-05-10'),
(89, 1, 123, 18908, 199, '2025-05-10'),
(90, 1, 123, 18908, 199, '2025-05-10'),
(91, 1, 53, 11970, 157, '2025-05-10'),
(92, 1, 53, 11970, 157, '2025-05-10'),
(93, 1, 159, 41433, 530, '2025-05-10'),
(94, 1, 159, 41433, 530, '2025-05-10'),
(95, 1, 181, 47664, 586, '2025-05-10'),
(96, 1, 181, 47664, 586, '2025-05-10'),
(97, 1, 564, 54781, 740, '2025-05-10'),
(98, 1, 564, 54781, 740, '2025-05-10'),
(99, 1, 224, 11040, 195, '2025-05-10'),
(100, 1, 224, 11040, 195, '2025-05-10'),
(101, 1, 86, 83718, 934, '2025-05-10'),
(102, 1, 86, 83718, 934, '2025-05-10'),
(103, 1, 73, 22631, 384, '2025-05-10'),
(104, 1, 73, 22631, 384, '2025-05-10'),
(105, 1, 48, 9277, 152, '2025-05-10'),
(106, 1, 48, 9277, 152, '2025-05-10'),
(107, 1, 119, 73085, 1270, '2025-05-10'),
(108, 1, 119, 73085, 1270, '2025-05-10'),
(109, 1, 160, 193567, 2194, '2025-05-10'),
(110, 1, 160, 193567, 2194, '2025-05-10'),
(111, 1, 162, 178546, 2107, '2025-05-10'),
(112, 1, 162, 178546, 2107, '2025-05-10'),
(113, 1, 272, 15340, 172, '2025-05-10'),
(114, 1, 272, 15340, 172, '2025-05-10'),
(115, 1, 7, 0, 0, '2025-05-10'),
(116, 1, 7, 0, 0, '2025-05-10'),
(117, 1, 5, 50, 1, '2025-05-10'),
(118, 1, 5, 50, 1, '2025-05-10'),
(119, 1, 8, 0, 0, '2025-05-10'),
(120, 1, 8, 0, 0, '2025-05-10'),
(121, 1, 12, 142, 2, '2025-05-10'),
(122, 1, 12, 142, 2, '2025-05-10'),
(123, 1, 4, 48, 0, '2025-05-10'),
(124, 1, 4, 48, 0, '2025-05-10'),
(125, 1, 6, 0, 0, '2025-05-10'),
(126, 1, 6, 0, 0, '2025-05-10'),
(127, 1, 16, 126, 2, '2025-05-10'),
(128, 1, 16, 126, 2, '2025-05-10'),
(129, 1, 10, 166, 2, '2025-05-10'),
(130, 1, 10, 166, 2, '2025-05-10'),
(131, 1, 42, 1932, 30, '2025-05-10'),
(132, 1, 42, 1932, 30, '2025-05-10'),
(133, 15, 377, 38185, 317, '2025-05-10'),
(134, 15, 377, 38185, 317, '2025-05-10'),
(135, 15, 65, 2447, 37, '2025-05-10'),
(136, 15, 65, 2447, 37, '2025-05-10'),
(137, 1, 551, 58360, 436, '2025-05-10'),
(138, 1, 551, 58360, 436, '2025-05-10');

-- --------------------------------------------------------

--
-- Table structure for table `leaderboard`
--

CREATE TABLE `leaderboard` (
  `leaderboard_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `best_survival_time` int(11) NOT NULL,
  `total_dmg_inflicted` int(11) NOT NULL,
  `rank` int(11) NOT NULL,
  `last_updated` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `leaderboard`
--

INSERT INTO `leaderboard` (`leaderboard_id`, `player_id`, `best_survival_time`, `total_dmg_inflicted`, `rank`, `last_updated`) VALUES
(1, 1, 564, 903020, 1, 1746866347),
(2, 2, 40, 3076, 6, 1745773995),
(3, 3, 196, 35744, 4, 1746763824),
(4, 4, 69, 14232, 5, 1746786806),
(5, 5, 431, 41290, 2, 1746768062),
(6, 15, 377, 40632, 3, 1746861165);

-- --------------------------------------------------------

--
-- Table structure for table `player`
--

CREATE TABLE `player` (
  `player_id` int(11) NOT NULL,
  `username` varchar(20) NOT NULL,
  `created_at` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `player`
--

INSERT INTO `player` (`player_id`, `username`, `created_at`) VALUES
(1, 'user', '2025-04-27'),
(2, 'player1', '2025-04-28'),
(3, 'kesha', '2025-05-08'),
(4, 'pl@yer4', '2025-05-09'),
(5, 'pl@yer6', '2025-05-09'),
(6, 'player@7', '2025-05-09'),
(7, 'player@8', '2025-05-09'),
(8, 'player@12', '2025-05-09'),
(9, 'player@13', '2025-05-09'),
(10, 'player@14', '2025-05-09'),
(11, 'player@15', '2025-05-09'),
(12, 'player@16', '2025-05-09'),
(13, 'player@17', '2025-05-09'),
(14, 'player@18', '2025-05-09'),
(15, 'TheOnlyException', '2025-05-10'),
(16, 'abds', '2025-05-10');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(80) NOT NULL,
  `password` varchar(80) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`) VALUES
(1, 'user', 'pass'),
(2, 'player1', 'player1'),
(3, 'kesha', 'kesha'),
(4, 'pl@yer2', 'thepl@yer2'),
(5, 'pl@yer3', 'thepl@yer3'),
(6, 'pl@yer4', 'thepl@yer4'),
(7, 'pl@yer5', 'theplayer@5'),
(8, 'pl@yer6', 'thepl@yer6'),
(9, 'player@7', 'theplayer@7'),
(10, 'player@8', 'theplayer@8'),
(11, 'player@9', 'theplayer@9'),
(12, 'player10', '@player10'),
(13, 'player@11', 'theplayer@11'),
(14, 'player@12', 'theplayer@12'),
(15, 'player@13', 'player@13'),
(16, 'player@14', 'player@14'),
(17, 'player@15', 'player@15'),
(18, 'player@16', 'player@16'),
(19, 'player@17', 'player@17'),
(20, 'player@18', 'player@18'),
(21, 'TheOnlyException', '@abcd123'),
(22, 'abds', 'abdsabds!!2');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `game_session`
--
ALTER TABLE `game_session`
  ADD PRIMARY KEY (`session_id`);

--
-- Indexes for table `leaderboard`
--
ALTER TABLE `leaderboard`
  ADD PRIMARY KEY (`leaderboard_id`);

--
-- Indexes for table `player`
--
ALTER TABLE `player`
  ADD PRIMARY KEY (`player_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `game_session`
--
ALTER TABLE `game_session`
  MODIFY `session_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=139;

--
-- AUTO_INCREMENT for table `leaderboard`
--
ALTER TABLE `leaderboard`
  MODIFY `leaderboard_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `player`
--
ALTER TABLE `player`
  MODIFY `player_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

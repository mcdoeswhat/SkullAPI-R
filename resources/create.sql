CREATE TABLE IF NOT EXISTS `%s`
(
    `id` BIGINT
(
    20
) NOT NULL AUTO_INCREMENT,
    `player` VARCHAR
(
    255
) NULL DEFAULT NULL,
    `signature` TEXT NULL DEFAULT NULL,
    `value` TEXT NULL DEFAULT NULL,
    UNIQUE INDEX `id`
(
    `id`
),
    UNIQUE INDEX `player`
(
    `player`
)
    )
    COLLATE ='utf8_general_ci'
    ENGINE=InnoDB
;
# Описание метаданных - типов узлов и связей графа и их свойст
Описание это пока сильно сказано, просто привожу здесь список, чтоб не забыть.

## Узлы

### Ent (Entity)
Любая сущность, базовый тип для всех других. 

Атрибуты
* uid - GUID - индексирован
* created - дата создания узла в системе - индексирован, ex: "2022-07-13T20:03:36.289863300"
* name - имя узла по умолчанию (может быть пустым)
* name_ru - тоже имя на русском (может быть пустым),  ex: "Кирилловка"
* name_uk - тоже имя на украинском (может быть пустым), ex: "Кирилівка",
* name_en - тоже имя на английском (может быть пустым), ex: "Kyrylivka"
* src - откуда получена информация о узле
  * osm - OpenStreetMap

### Place
Место, локация, населенный пункт

Родитель: [Ent](#Ent) от которого наследует все атрибуты

Атрибуты 
 * osm_name_prefix - ex: "село", см [:name:prefix](https://wiki.openstreetmap.org/wiki/Key:name:prefix)
 * osm_place - ex: "village", см [place](https://wiki.openstreetmap.org/wiki/RU:Key:place)
 * osm_admin_level - ex: 9, число, см [admin_level](https://wiki.openstreetmap.org/wiki/Key:admin_level)
 * osm_boundary - ex: "administrative", см [boundary](https://wiki.openstreetmap.org/wiki/Key:boundary)
 * osm_population: ex: 732, число, см [population](https://wiki.openstreetmap.org/wiki/Key:population)
 
### Photo
Фотография

Родитель: [Ent](#Ent) от которого наследует все атрибуты

### Person
Человек

Родитель: [Ent](#Ent) от которого наследует все атрибуты


## Связи
Общие атрибуты для всех связей       
 * uid
 * src
 * created


### CONTAINS
(Субъект) -[содержит]-> (Объект)

Связь отражает вхождение с владением одного в другое. Субъект как что-то более большое и значимое содержит внутри себя
менее значимый объект.

([Place](#Place)) - [CONTAINS] -> ([Place](#Place))




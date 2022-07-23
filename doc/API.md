# Описание API

## Загрузка разобранных фото документов
Загрузка фото и описаний в два этапа
POST /doc/photo

Возвращает: 
uuid

POST /doc/photo/<uuid>/attr
BODY

```json
{
  "tags": [
    "#Tag1",
    "#Tag2"
  ],
  "description": "Описание фото тут",
  "location": {
    "point": {
      "lat": 41.123,
      "lon": 33.123
    }
  },
  "content" : [
    {
      "type" : "person",
      "rect": [10, 10, 120, 200],
      "name": "Иванов Иван Иванович или пусто",
      "uid": "guidhere-guid-here-guid-guidhere"
    }
  ]
}
```



package com.facultytimetable.pro.data.local.db.converter

import androidx.room.TypeConverter
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.WeekType

class Converters {

    @TypeConverter
    fun fromSubjectType(type: SubjectType): String = type.name

    @TypeConverter
    fun toSubjectType(value: String): SubjectType = SubjectType.valueOf(value)

    @TypeConverter
    fun fromRoomType(type: RoomType): String = type.name

    @TypeConverter
    fun toRoomType(value: String): RoomType = RoomType.valueOf(value)

    @TypeConverter
    fun fromSlotType(type: SlotType): String = type.name

    @TypeConverter
    fun toSlotType(value: String): SlotType = SlotType.valueOf(value)

    @TypeConverter
    fun fromWeekType(type: WeekType): String = type.name

    @TypeConverter
    fun toWeekType(value: String): WeekType = WeekType.valueOf(value)
}

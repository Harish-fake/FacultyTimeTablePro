package com.facultytimetable.pro.data.local.db.converter

import androidx.room.TypeConverter
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.WeekType

class Converters {

    @TypeConverter
    fun fromSubjectType(value: SubjectType): String = value.name

    @TypeConverter
    fun toSubjectType(value: String): SubjectType = SubjectType.valueOf(value)

    @TypeConverter
    fun fromRoomType(value: RoomType): String = value.name

    @TypeConverter
    fun toRoomType(value: String): RoomType = RoomType.valueOf(value)

    @TypeConverter
    fun fromSlotType(value: SlotType): String = value.name

    @TypeConverter
    fun toSlotType(value: String): SlotType = SlotType.valueOf(value)

    @TypeConverter
    fun fromWeekType(value: WeekType): String = value.name

    @TypeConverter
    fun toWeekType(value: String): WeekType = WeekType.valueOf(value)

    @TypeConverter
    fun fromBooleanList(value: List<Boolean>): String = value.joinToString(",")

    @TypeConverter
    fun toBooleanList(value: String): List<Boolean> = value.split(",").map { it.toBoolean() }
}

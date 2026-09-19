package com.xpad.mapper.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.xpad.mapper.R

class HelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = ScrollView(requireContext()).apply {
            isFillViewport = true
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        val tv = TextView(requireContext()).apply {
            textSize = 15f
            setLineSpacing(dp(4).toFloat() / 2f, 1f)
            movementMethod = LinkMovementMethod.getInstance()
            text = HtmlCompat.fromHtml(HELP_HTML, HtmlCompat.FROM_HTML_MODE_COMPACT)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
        root.addView(tv)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // чтобы скролл не сбрасывался
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val HELP_HTML = """
        <h2>Геймпад виден как «клавиатура»</h2>
        <p>Это самая частая жалоба. Многие беспроводные геймпады даже в режиме «Gamepad» подключаются по Bluetooth как <b>HID-клавиатура</b> — Android заводит их в раздел «Физические клавиатуры», и приложение (и игры) видят клавиши вместо геймпада.</p>
        <ul>
            <li>В приложении такой геймпад теперь виден в списке «Устройств» с красной плашкой «Клавиатура».</li>
            <li>Решения: зажать <b>MODE/Home 5–10 сек</b> для переподключения в игровой HID; комбинация <b>Start+вниз</b> (или Start+крестовина); или подключить по <b>USB-кабелю</b> — по проводу он почти всегда поднимает правильный gamepad-профиль.</li>
            <li>Проверь во вкладке «Тест»: если видны события «RAW …», значит связь и провода/элементы работают, а проблема только в HID-профиле.</li>
        </ul>
        <h2>Почему игры не видят твой геймпад</h2>
        <p>Android — не Windows. На ПК драйвер x360ce «подсовывал» играм виртуальный Xbox-контроллер и переназначал кнопки твоего геймпада. В Android нет публичного API для подмены контроллера одним приложением для всех программ — это возможно только на «прокачанных» прошивках или с root (vHID-драйвер).</p>
        <p>Поэтому реальная причина «ни одна игра не видит геймпад» обычно одна из двух:</p>
        <ul>
            <li><b>Режим контроллера.</b> Беспроводные геймпады имеют 2–3 HID-режима. В режиме <b>DX/Android/Xbox (Gamepad, HID)</b> Android обязан распознать геймпад. В режиме <b>Joystick/Dinput/«PS»</b> система видит только оси — и большинство игр такой джойстик игнорируют.</li>
            <li><b>Раскладка кнопок.</b> Даже в режиме Xbox часть кнопок может приходить как Button 1..16 (типично для бюджетных геймпадов) — в этом случае игра не понимает, что нажата кнопка «крестовина» или «A».</li>
        </ul>
        <h2>Что делать</h2>
        <ol>
            <li>Зайди во вкладку <b>Устройства</b> и посмотри, что показывает приложение. Красный/жёлтый диагноз = контроллер виден не как геймпад.</li>
            <li>Переключи геймпад в режим Xbox/Gamepad. Обычно: зажать <b>HOME (кнопка с логотипом) 3–5 сек</b>, либо комбинация <b>Start + D-пада</b>, либо <b>Home + A</b> (смотри инструкцию к твоему контроллеру).</li>
            <li>После смены режима отключи и снова подключи геймпад, затем нажми «Обновить».</li>
            <li>Зайди во вкладку <b>Тест</b> — покликай кнопки. Если подсвечиваются не те кнопки — создай профиль ремапа во вкладке <b>Профили</b>.</li>
            <li>Если в тесте кнопки есть, а в игре всё равно нет — проверь настройки игры: во многих играх/эмуляторах есть отдельный пункт «Controller / Геймпад», где жмут «Map»/«Assign» и заново учат кнопки.</li>
        </ol>
        <h2>Включить эмуляцию как на ПК</h2>
        <p>Полноценная подмена контроллера (как x360ce) на Android требует root + vHID. Приложение честно показывает: доступен root или нет — во вкладке <b>Профили</b>. Без root профили работают как инструмент проверки раскладки и настройки дед-зон.</p>
        <h2>Горячие советы</h2>
        <ul>
            <li>Эмуляторы (RetroArch, PPSSPP, DuckStation) имеют собственный ввод — там контроллер обычно виден всегда, если подключить в режиме Xbox.</li>
            <li>Игры от Game Pass/Xbox Cloud вообще не видят физические геймпады по Bluetooth на Android без специальных сборок.</li>
            <li>Если кнопок нет вовсе — попробуй кабель USB (режим проводного геймпада) — Wi-Fi/Bluetooth некоторые устройства отключают HID.</li>
        </ul>
        <p><i>Disclaimer: это не замена официального драйвера x360ce — на Android нет его точного аналога без root.</i></p>
        """
            .trimIndent()
    }
}
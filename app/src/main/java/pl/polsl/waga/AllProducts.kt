package pl.polsl.waga

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class AllProducts : AppCompatActivity() {
    private var labelsList = arrayListOf("Jabłko", "Banan", "Karambola", "Guawa", "Kiwi","Mango", "Melon",
        "Pomarancza", "Brzoskwinia", "Gruszka", "Persymona", "Papaja", "Sliwka", "Granat")
    private var labelsNumber = labelsList.size
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_products)


        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
        }

        createLayoutDynamically(labelsNumber);
    }
    private fun createLayoutDynamically(buttonsNumber: Int) {
        val baseLayout = findViewById<LinearLayout>(R.id.linearLayout)

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        for (i in 0..buttonsNumber) {
            val layoutH2 = LinearLayout(this)
            baseLayout.addView(layoutH2)

            for(j in 0..2)
            {
                if(i*3+j == buttonsNumber)
                {
                    return
                }

                var myButton: ImageButton = ImageButton(this);

                myButton.setImageResource(R.drawable.apple75);
                myButton.setId(i*3 + j);

                myButton.x = 10.0f + j*25.0f
                myButton.y = 10.0f
                var id_ = myButton.getId();

                layoutH2.addView(myButton);

                myButton.setOnClickListener {
                  /*  Toast.makeText(this,
                        "Button clicked index = " + id_, Toast.LENGTH_SHORT)
                        .show();*/
                    val toast = Toast.makeText(applicationContext, "Drukowanie etykiety dla Jabłko" , Toast.LENGTH_SHORT)
                    toast.show()
                    val myIntent = Intent(this, MainActivity::class.java)
                    startActivity(myIntent)

                }
            }
        }
    }
}
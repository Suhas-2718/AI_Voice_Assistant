from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv
import os
import google.generativeai as genai
from datetime import datetime

load_dotenv()

app = Flask(__name__)
CORS(app)

api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("API key not found")

genai.configure(api_key=api_key)
model = genai.GenerativeModel("gemini-1.5-flash")

@app.route("/healthz", methods=["GET"])
def healthz():
    return jsonify({"status": "ok"}), 200

@app.route("/ask", methods=["POST"])
def ask():
    print("Request received!")
    data = request.json
    print("User input:", data)
    return jsonify({"reply": "Hello from backend!"})

@app.route("/chat", methods=["POST"])
def chat():
    try:
        data = request.json or {}
        user_message = data.get("message")
        if not user_message:
            return jsonify({"error": "No message provided"}), 400

        if "today's date" in user_message.lower():
            today = datetime.now().strftime("%B %d, %Y")
            return jsonify({"reply": f"Today is {today}."})
        if "time" in user_message.lower():
            now = datetime.now().strftime("%I:%M %p")
            return jsonify({"reply": f"The current time is {now}."})

        prompt = f"Reply concisely in one sentence, maximum 30 letters: {user_message}"
        response = model.generate_content(prompt)

        print("Gemini raw response object:", response)

        reply_text = getattr(response, "text", None)
        if not reply_text:
            reply_text = "Sorry, I didn’t get that."
        else:
            reply_text = reply_text.strip()[:40]

        print("Final reply sent to client:", reply_text)
        return jsonify({"reply": reply_text})

    except Exception as e:
        print("Gemini API error:", e)
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=True)
